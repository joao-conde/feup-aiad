package main;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

import main.Purchase;
import utilities.BuyerStatistics;
import utilities.MarketLogger;
import utilities.Utils;

import java.util.concurrent.ConcurrentHashMap;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SSIteratedContractNetResponder;
import jade.proto.SSResponderDispatcher;
import jade.util.Logger;

public class BuyerAgent extends Agent {

	private static final long serialVersionUID = 1L;
	private int ticksNoActivity = 0;
	private BuyerStatistics statManager;
	private Logger logger;
	private String agentName;
	private DFAgentDescription dfd;
	private ArrayList<Purchase> purchases = new ArrayList<Purchase>();
	private ConcurrentHashMap<String, Float> items = new ConcurrentHashMap<String, Float>(); // itemID, maxValue
	protected ConcurrentHashMap<String, Float> ratings = new ConcurrentHashMap<String, Float>(); // sellerID,
																									// averageRating
	private ConcurrentHashMap<String, ArrayList<String>> liveAuctions = new ConcurrentHashMap<String, ArrayList<String>>(); // itemID,
																															// list
																															// of
																															// sellers
	private ConcurrentHashMap<String, RatingInfo> awaitingRate = new ConcurrentHashMap<String, RatingInfo>(); // itemID,
																												// rating
																												// information

	private void removeFromLiveAuctions(String itemID, String sender) {
		ArrayList<String> sellers;
		if ((sellers = liveAuctions.get(itemID)) != null) {
			int index = sellers.indexOf(sender);
			if (index != -1)
				sellers.remove(index);
		}
	}

	private float computeAverageRating(String sellerID) {
		float sumRatings = 0, sellerPurchases = 0;
		for (Purchase p : purchases) {
			if (p.getSellerID().equals(sellerID)) {
				sellerPurchases++;
				sumRatings += p.getRating();
			}
		}
		return (float) (sellerPurchases == 0 ? 0.8 : sumRatings / sellerPurchases);
	}

	protected void setup() {

		Object[] args = this.getArguments();
		dfd = new DFAgentDescription();
		dfd.setName(getAID());

		agentName = this.getLocalName();

		logger = MarketLogger.createLogger(this.getClass().getName(), agentName);
		//logger.fine(agentName + " is now active in the market");

		for (Object arg : args) {
			SimpleEntry<String, Float> argument = (SimpleEntry<String, Float>) arg;
			items.put(argument.getKey(), argument.getValue());

			ServiceDescription sd = new ServiceDescription();
			sd.setType(Utils.SD_BUY);
			sd.setName(argument.getKey());
			dfd.addServices(sd);
		}
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		statManager = new BuyerStatistics(agentName, items.size());
		
		addBehaviour(new InformDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
		addBehaviour(new CFPDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
		addBehaviour(new QueryDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF)));
		addBehaviour(new CheckEnd(this, 2000, 3));
	}

	private class CFPDispatcher extends SSResponderDispatcher {

		private static final long serialVersionUID = 1L;

		public CFPDispatcher(Agent a, MessageTemplate tpl) {
			super(a, tpl);
		}

		@Override
		protected Behaviour createResponder(ACLMessage cfp) {
			return new FIPAIteratedContractedNet(myAgent, cfp);
		}

		private class FIPAIteratedContractedNet extends SSIteratedContractNetResponder {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public FIPAIteratedContractedNet(Agent a, ACLMessage cfp) {
				super(a, cfp);
				registerHandleCfp(new HandleCFP(this));
			}

			private class HandleCFP extends SimpleBehaviour {

				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				private boolean finished = false;
				private FIPAIteratedContractedNet fipacn;

				public HandleCFP(FIPAIteratedContractedNet fipaIteratedContractedNet) {
					super();
					fipacn = fipaIteratedContractedNet;
				}

				@Override
				public void action() {
					ACLMessage cfp = (ACLMessage) fipacn.getDataStore().get(CFP_KEY);
					handleCFP(cfp);
				}

				@Override
				public boolean done() {
					return finished;
				}

				private void handleCFP(ACLMessage cfp) {

					Bid receivedBid;
					try {
						receivedBid = (Bid) cfp.getContentObject();

						/*logger.fine(agentName + " received a CFP from " + cfp.getSender().getLocalName() + " to bid on "
								+ receivedBid.getItem());*/

						if (!items.containsKey(receivedBid.getItem())) {
							//logger.fine(agentName + " is not looking for " + receivedBid.getItem() + " anymore");
							ACLMessage reply = cfp.createReply();
							reply.setPerformative(ACLMessage.REFUSE);
							sendReply(reply);
							return;
						}

						if (cfp.getProtocol() == Utils.CFP_PROTOCOL) {
							ticksNoActivity = 0;
							String sellerID = cfp.getSender().getLocalName();
							ratings.put(sellerID, computeAverageRating(sellerID));
							ArrayList<String> sellers;
							if ((sellers = liveAuctions.get(receivedBid.getItem())) == null) {
								sellers = new ArrayList<String>();
								sellers.add(sellerID);
								liveAuctions.put(receivedBid.getItem(), sellers);
							} else {
								sellers.add(sellerID);
							}

							AID[] buyers = fetchOtherBuyers();
							ACLMessage askRates = new ACLMessage(ACLMessage.QUERY_IF);
							askRates.setContent(sellerID);
							askRates.setProtocol(Utils.RATE);

							for (AID buyerAID : buyers)
								askRates.addReceiver(buyerAID);
							
							//logger.fine(agentName + " asking other buyers for their opinion on seller " + sellerID);
							
							send(askRates);
							awaitingRate.put(sellerID, new RatingInfo(buyers.length));
							cfp.setProtocol(null);
							addBehaviour(new WaitForRatings(this.myAgent, cfp, sellerID));
						} else
							handleRegularCFP(cfp);
					} catch (UnreadableException e) {
						e.printStackTrace();
					}

				}

				private void handleRegularCFP(ACLMessage cfp) {
					try {
						ACLMessage reply = cfp.createReply();
						Bid receivedBid = (Bid) cfp.getContentObject();
						String lastBidderName = null, sellerID = cfp.getSender().getLocalName();

						reply.setPerformative(ACLMessage.PROPOSE);

						//logger.fine(agentName + " rates " + sellerID + " with " + ratings.get(sellerID));

						if ((lastBidderName = receivedBid.getLastBidder()) != null) {

							if (!lastBidderName.equals(agentName)) {

								if (receivedBid.getValue()
										+ receivedBid.getMinIncrease() <= items.get(receivedBid.getItem())
												* ratings.get(cfp.getSender().getLocalName())) {
									receivedBid.setNewValue(
											Utils.round(receivedBid.getValue() + receivedBid.getMinIncrease(), 3));
									/*logger.fine(agentName + " will bid " + receivedBid.getValue() + " on "
											+ receivedBid.getItem());*/
								} else {
									//logger.fine(agentName + " is dropping out of " + receivedBid.getItem() + " auction");
									reply.setPerformative(ACLMessage.REFUSE);
									removeFromLiveAuctions(receivedBid.getItem(), cfp.getSender().getLocalName());
								}
							}

						} else {
							// first bid (cfp call)
							if (receivedBid.getValue() > items.get(receivedBid.getItem())
									* ratings.get(cfp.getSender().getLocalName())) {
								/*logger.fine(agentName + " will not even begin to bid on auction " + receivedBid.getItem()
										+ " because price is too high already");*/
								reply.setPerformative(ACLMessage.REFUSE); // first value is already too high
								removeFromLiveAuctions(receivedBid.getItem(), cfp.getSender().getLocalName());
							}
						}
						reply.setContentObject(receivedBid);
						
						/*logger.fine(agentName + " reply to " + receivedBid.getItem() + " from "
								+ cfp.getSender().getLocalName() + " with " + ACLMessage.getPerformative(reply.getPerformative()) + " and value "
								+ receivedBid.getValue());*/
						
						if(reply.getPerformative() == ACLMessage.PROPOSE)
							statManager.incItemAttempts(receivedBid.getItem(), cfp.getSender().getLocalName());
						
						sendReply(reply);
					} catch (UnreadableException | IOException e) {
						e.printStackTrace();
					}

				}

				private void sendReply(ACLMessage reply) {
					fipacn.getDataStore().put(fipacn.REPLY_KEY, reply);
					finished = true;
				}

				private class WaitForRatings extends SimpleBehaviour {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;
					private String sellerID;
					private boolean finished = false;
					private ACLMessage cfp;

					public WaitForRatings(Agent a, ACLMessage cfp, String sellerID) {
						super(a);
						this.sellerID = sellerID;
						this.cfp = cfp;
					}

					@Override
					public void action() {
						Float opinion;
						if ((opinion = awaitingRate.get(sellerID).calculateAverageRating()) == null) {
							block();
						} else {
							String log = agentName + " got opinions from other buyers";
							if (opinion != -1) {
								if (ratings.containsKey(sellerID))
									ratings.put(sellerID, (float) (0.3 * opinion + 0.7 * ratings.get(sellerID)));
								else
									ratings.put(sellerID, (float) (0.5 * opinion + 0.5 * 0.8));
								
								log += " and took their rating into account";
							}
							else log += " but no one had a well-formed opinion";
							
							//logger.fine(log);
							handleRegularCFP(cfp);
							finished = true;
						}
					}

					public boolean done() {
						return finished;
					}
				}
			}

			protected AID[] fetchOtherBuyers() {
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType(Utils.SD_BUY);
				template.addServices(sd);

				AID[] buyers = null;
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					buyers = new AID[result.length - 1];
					int a = 0;
					for (int i = 0; i < result.length; ++i) {
						if (!result[i].getName().equals(this.getAgent().getAID())) {
							buyers[a] = result[i].getName();
							a++;
						}

					}

				} catch (FIPAException fe) {
					fe.printStackTrace();
				}
				return buyers;
			}

			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
				
				try {
					Bid bid = (Bid) accept.getContentObject();
					//logger.fine(agentName + " received an ACCEPT_PROPOSAL on " + bid.getItem() + " from seller " + accept.getSender().getLocalName());

					removeFromLiveAuctions(bid.getItem(), cfp.getSender().getLocalName());
					
					statManager.incItemAuctionsWon(bid.getItem());
					
					if (!items.containsKey(bid.getItem()))
						return null;
					
					Purchase purchase = new Purchase(bid, accept.getSender().getLocalName(),
							Integer.parseInt(accept.getUserDefinedParameter("DeliveryTime")), items.get(bid.getItem()));

					for (Purchase p : purchases) {
						if (p.getItemID().equals(purchase.getItemID())) {
							if(p.getValuePaid() == purchase.getValuePaid()) {
								if (ratings.get(p.getSellerID()) < ratings.get(purchase.getSellerID())) {
									purchases.remove(p);
									purchases.add(purchase);
									return null;
								}
							}else if(p.getValuePaid() > purchase.getValuePaid() ) {
								purchases.remove(p);
								purchases.add(purchase);
								return null;
							}
							
						}
					}
					purchases.add(purchase);

				} catch (UnreadableException e) {
					e.printStackTrace();
				}

				return null;
			}

		}

	}

	private class QueryDispatcher extends SSResponderDispatcher {

		private static final long serialVersionUID = 1L;

		public QueryDispatcher(Agent a, MessageTemplate tpl) {
			super(a, tpl);
		}

		@Override
		protected Behaviour createResponder(ACLMessage query) {
			return new QueryHandler(myAgent, query);
		}

		private class QueryHandler extends Behaviour {

			private static final long serialVersionUID = 1L;
			ACLMessage msg;
			Boolean finished = false;

			public QueryHandler(Agent myAgent, ACLMessage msg) {
				this.msg = msg;
				this.myAgent = myAgent;
			}

			@Override
			public void action() {
				if (msg == null) {
					return;
				}

				if (msg.getProtocol() == Utils.RATE)
					handleRatingRequests();
				else
					handlePurchaseConfirmations();
			}

			private void handleRatingRequests() {
				ACLMessage reply = msg.createReply();
				String sellerID = msg.getContent();
				SimpleEntry<String, String> content;

				/*logger.fine(agentName + " received a request for his opinion on seller " + sellerID 
						+ " by buyer " + msg.getSender().getLocalName()); */
				
				reply.setPerformative(ACLMessage.INFORM);
				reply.setProtocol(Utils.RATE);

				if (ratings.containsKey(sellerID)) {
					ratings.put(sellerID, computeAverageRating(sellerID));
					content = new SimpleEntry<String, String>(sellerID, ratings.get(sellerID).toString());
				} else {
					content = new SimpleEntry<String, String>(sellerID, Utils.NULL);
				}

				try {
					reply.setContentObject(content);
					send(reply);
				} catch (IOException e) {
					e.printStackTrace();
				}
				finished = true;
			}

			public void handlePurchaseConfirmations() {
				ACLMessage reply;
				finished = true;
				reply = this.msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);

				try {
					Bid receivedBid = (Bid) msg.getContentObject();
					if (!items.containsKey(receivedBid.getItem())) {
						/*logger.fine("Canceling purchase of " + receivedBid.getItem() + " to seller"
								+ msg.getSender().getLocalName());*/
						reply.setContent(Utils.CANCEL);
					} else {
						ArrayList<String> sellers;
						if ((sellers = liveAuctions.get(receivedBid.getItem())) != null && !sellers.isEmpty()) {
							finished = false;
							reply.setContent(Utils.WAIT);
							send(reply);
							return;
						}

						for (Purchase p : purchases) {
							if (p.getItemID().equals(receivedBid.getItem())
									&& p.getSellerID().equals(msg.getSender().getLocalName())) {
								reply.setContent(Utils.PURCHASE);
								/*logger.fine("Confirming purchase of " + receivedBid.getItem() + " to seller"
										+ msg.getSender().getLocalName());*/
								items.remove(p.getItemID());
								liveAuctions.remove(p.getItemID());
															
								ServiceDescription sd = new ServiceDescription();
								sd.setType(Utils.SD_BUY);
								sd.setName(p.getItemID());
								dfd.removeServices(sd);
								
								statManager.addPurchase(p);
								send(reply);
								return;
							}
						}
						/*logger.fine("Canceling purchase of " + receivedBid.getItem() + " to seller"
								+ msg.getSender().getLocalName());*/
						reply.setContent(Utils.CANCEL);

					}
					send(reply);
					return;
				} catch (UnreadableException e) {
					e.printStackTrace();
				}

				block();
			}

			@Override
			public boolean done() {
				return this.finished;
			}
		}

	}

	private class InformDispatcher extends SSResponderDispatcher {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public InformDispatcher(Agent a, MessageTemplate tpl) {
			super(a, tpl);
		}

		@Override
		protected Behaviour createResponder(ACLMessage inform) {
			return new InformHandler(myAgent, inform);
		}

		private class InformHandler extends Behaviour {

			private static final long serialVersionUID = 1L;

			ACLMessage msg;
			boolean finished = false;

			public InformHandler(Agent myAgent, ACLMessage inform) {
				this.msg = inform;
				this.myAgent = myAgent;
			}

			@Override
			public void action() {

				try {
					SimpleEntry<String, String> content = (SimpleEntry<String, String>) msg.getContentObject();
					if (awaitingRate.containsKey(content.getKey())) {
						awaitingRate.get(content.getKey()).processRatingInfo(content.getValue());
					}
				} catch (UnreadableException e) {
					e.printStackTrace();
				}

				finished = true;
			}

			@Override
			public boolean done() {
				return finished;
			}

		}
	}

	private class CheckEnd extends TickerBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int maxTickers;

		public CheckEnd(Agent a, long period, int maxTickers) {
			super(a, period);
			this.maxTickers = maxTickers;
		}

		@Override
		protected void onTick() {
			ticksNoActivity++;
			if(items.isEmpty()) {
				/*logger.fine(agentName + " has bought everything he wanted. Exiting the market");
				statManager.logStatistics(logger);*/
				informSimulatorAgent();
				try {
					DFService.deregister(myAgent);
				} catch (FIPAException e) {
					e.printStackTrace();
				}
				myAgent.doDelete();
			}
			else if(ticksNoActivity > maxTickers) {
				/*logger.fine(agentName + " has waited long enough. Exiting the market");
				statManager.logStatistics(logger);*/
				informSimulatorAgent();
				try {
					DFService.deregister(myAgent);
				} catch (FIPAException e) {
					e.printStackTrace();
				}
				myAgent.doDelete();
			}
		}
	}

	protected void informSimulatorAgent() {
		
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(Utils.SIMULATOR_AGENT);
		sd.setName("Simulator");
		template.addServices(sd);

		try {
			DFAgentDescription[] result = DFService.search(this, template);			
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.setSender(getAID());
			message.addReceiver(result[0].getName());
			
			this.send(message);					
		} 
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

	}


}