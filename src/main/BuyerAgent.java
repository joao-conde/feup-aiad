package main;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

import main.Purchase;
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

public class BuyerAgent extends Agent{


	private static final long serialVersionUID = 1L;
	private Logger logger;
	private String agentName;
	private DFAgentDescription dfd;
	private ArrayList<Purchase> purchases = new ArrayList<Purchase>();
	private ConcurrentHashMap<String,Float> items = new ConcurrentHashMap<String,Float>(); //itemID, maxValue
	protected ConcurrentHashMap<String,Float> ratings = new ConcurrentHashMap<String,Float>(); //sellerID, averageRating
	private ConcurrentHashMap<String, ArrayList<String>> liveAuctions = new ConcurrentHashMap<String,ArrayList<String>>(); //itemID, list of sellers
	private ConcurrentHashMap<String, RatingInfo> awaitingRate = new ConcurrentHashMap<String, RatingInfo>(); //itemID, rating information object
	
	private float computeAverageRating(String sellerID) {
		float sumRatings = 0, sellerPurchases = 0;
		for(Purchase p: purchases) {
			logger.fine(p.getItemID());
			if(p.getSellerID().equals(sellerID)) {
				sellerPurchases++;
				sumRatings += p.getRating();
			}
		}
		return (float) (sellerPurchases == 0 ? 0.8 : sumRatings/sellerPurchases);
	}
	
	protected void setup() {	
		
		Object[] args = this.getArguments();
		dfd = new DFAgentDescription();
		dfd.setName(getAID());
		
		agentName = this.getLocalName();
		
		logger = Utils.createLogger(this.getClass().getName(), agentName);
		
		for(Object arg: args) {
			SimpleEntry<String,Float> argument = (SimpleEntry<String,Float>) arg;
			items.put(argument.getKey(), argument.getValue());
			
			ServiceDescription sd = new ServiceDescription();
			sd.setType(Utils.SD_BUY);
			sd.setName(argument.getKey());
			dfd.addServices(sd);
		}
	    try {
	      DFService.register(this, dfd);
	    }
	    catch (FIPAException fe) {
	      fe.printStackTrace();
	    }
	    
	    addBehaviour(new InformDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
	    addBehaviour(new CFPDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
	    addBehaviour(new QueryDispatcher(this,MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF)));
	    addBehaviour(new CheckEnd(this, 5000, 3));
	}
	
	private class CFPDispatcher extends SSResponderDispatcher {


		private static final long serialVersionUID = 1L;


		public CFPDispatcher(Agent a, MessageTemplate tpl) {
			super(a, tpl);
		}


		@Override
		protected Behaviour createResponder(ACLMessage cfp) {
			return new FIPAIteratedContractedNet(myAgent,cfp);
		}
		
		private class WaitForRatings extends SimpleBehaviour{
			
			private String sellerID;
			private boolean finished = false;
			
			public WaitForRatings(Agent a, String sellerID) {
				super(a);
				this.sellerID = sellerID;
			}
			
			@Override
			public void action() {
				Float opinion;
				if((opinion = awaitingRate.get(sellerID).calculateAverageRating()) == null) {
					block();
				}else {		
					System.out.println("OPINION: " + opinion);
					System.out.println("RATING ANTES DOS PEDIDOS " + ratings.get(sellerID));
					if(opinion != -1) {						
						if(ratings.containsKey(sellerID)) 
							ratings.put(sellerID, (float)(0.3 * opinion + 0.7 * ratings.get(sellerID)));
						else
							ratings.put(sellerID, (float)(0.5 * opinion + 0.5 * 0.8));	
					}
					
					System.out.println("RATING DEPOIS DOS PEDIDOS " + ratings.get(sellerID));
					finished = true;
				}
			}
			
			public boolean done() {
				return finished;
			}

		}
		
		private class FIPAIteratedContractedNet extends SSIteratedContractNetResponder {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public FIPAIteratedContractedNet(Agent a, ACLMessage cfp) {
				super(a, cfp);
			}
			
			protected ACLMessage handleCfp(ACLMessage cfp) {
				
				ACLMessage reply = cfp.createReply();
						
				try {
					Bid receivedBid = (Bid) cfp.getContentObject();
					
					logger.fine(agentName+": Received a CFP from "+cfp.getSender().getLocalName()+" to "+receivedBid.getItem());
					if(!items.containsKey(receivedBid.getItem())) {
						logger.fine("Not looking for item anymore");
						reply.setPerformative(ACLMessage.REFUSE);
						return reply;
					}
					
					if(cfp.getProtocol() == Utils.CFP_PROTOCOL) {
						logger.fine("CFP");
						String sellerID = cfp.getSender().getLocalName();
						ratings.put(sellerID, computeAverageRating(sellerID));
						logger.fine("First CFP, calculating average rating for seller " + sellerID);
						ArrayList<String> sellers;
						if((sellers = liveAuctions.get(receivedBid.getItem())) == null) {
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
						
						for(AID buyerAID: buyers) askRates.addReceiver(buyerAID);
						
						awaitingRate.put(sellerID, new RatingInfo(buyers.length));
						
						addBehaviour(new WaitForRatings(this.myAgent, sellerID));
						
						send(askRates);					
					}
					
					String lastBidderName = null;
					reply.setPerformative(ACLMessage.PROPOSE);
					
					if((lastBidderName = receivedBid.getLastBidder()) != null) {
						
						//This agent is not the current winner
						logger.fine(agentName+": LastHighest was "+lastBidderName + " at " + receivedBid.getValue());
						if(!lastBidderName.equals(agentName)) {
							
							logger.fine(receivedBid.getItem() + " ITEM RATE OF SELLER " + ratings.get(cfp.getSender().getLocalName()));
							if(receivedBid.getValue() + receivedBid.getMinIncrease() <= items.get(receivedBid.getItem()) * ratings.get(cfp.getSender().getLocalName()))
								receivedBid.setNewValue(Utils.round(receivedBid.getValue()+receivedBid.getMinIncrease(), 3));
							else {
								logger.fine("REFUSE");
								reply.setPerformative(ACLMessage.REFUSE);
								ArrayList<String> sellers;
								if((sellers = liveAuctions.get(receivedBid.getItem())) != null) {
									sellers.remove(sellers.indexOf(cfp.getSender().getLocalName()));
								}
									
							}
						}
						
					} else {
						//first bid (cfp call)
						if(receivedBid.getValue() > items.get(receivedBid.getItem()) * ratings.get(cfp.getSender().getLocalName())) {
							float value = ratings.get(cfp.getSender().getLocalName());
							logger.fine("FIRST CFP REFUSE");
							reply.setPerformative(ACLMessage.REFUSE); //first value is already too high
							ArrayList<String> sellers;
							if((sellers = liveAuctions.get(receivedBid.getItem())) != null) {
								sellers.remove(sellers.indexOf(cfp.getSender().getLocalName()));
							}
						}
					}
					reply.setContentObject(receivedBid);
					logger.fine(agentName+": Propose to "+receivedBid.getItem()+" from "+ cfp.getSender().getLocalName() +" with "+reply.getPerformative()+" and value "+ receivedBid.getValue());
					
				} catch (UnreadableException | IOException e) {
					e.printStackTrace();
				}
				
				return reply;
			}
			
			protected AID[] fetchOtherBuyers() {
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType(Utils.SD_BUY);
				template.addServices(sd);
				
				AID[] buyers = null;
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					buyers = new AID[result.length-1];
					int a = 0;
					for (int i = 0; i < result.length; ++i){
						if(!result[i].getName().equals(this.getAgent().getAID())) {
							buyers[a] = result[i].getName();
							a++;
						}

					}
					
				} 
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
				return buyers;
			}

			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
				try {
					Bid bid = (Bid)accept.getContentObject();
					
					ArrayList<String> sellers;
					if((sellers = liveAuctions.get(bid.getItem())) != null) {
						sellers.remove(sellers.indexOf(accept.getSender().getLocalName()));
					}
					
					if(!items.containsKey(bid.getItem()))
						return null;
					Purchase purchase = new Purchase(bid, 
							accept.getSender().getLocalName(), 
							Integer.parseInt(accept.getUserDefinedParameter("DeliveryTime")), 
							items.get(bid.getItem()));
					System.out.println("--------------------->>>>>>CALCULATED RATING " + purchase.getRating());
					for(Purchase p: purchases) {
						if(p.getItemID().equals(purchase.getItemID())) {
							if(p.getRating() < purchase.getRating()) {
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
					if(msg == null) {
						return;
					}
					
					if(msg.getProtocol() == Utils.RATE.toString())
						handleRatingRequests();
					else
						handlePurchaseConfirmations();
					
					//finished = true;
			}
			
			private void handleRatingRequests() {
				
				ACLMessage reply = msg.createReply();
				String sellerID = msg.getContent();
				SimpleEntry<String, String> content;
				
				BuyerAgent parent = (BuyerAgent)this.myAgent;
				
				reply.setPerformative(ACLMessage.INFORM);
				reply.setProtocol(Utils.RATE);
				
				if(parent.ratings.contains(sellerID)) {
					parent.computeAverageRating(sellerID);
					content = new SimpleEntry<String, String>(sellerID, ratings.get(sellerID).toString());
				}
				else {
					content = new SimpleEntry<String, String>(sellerID, Utils.NULL);
				}
				
				try {
					reply.setContentObject(content);
					send(reply);
				} catch (IOException e) {
					e.printStackTrace();
				}
				finished=true;
			}

			public void handlePurchaseConfirmations() {
				ACLMessage reply;
				finished = true;
				reply = this.msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				
				try {
					Bid receivedBid = (Bid) msg.getContentObject();
					if(!items.containsKey(receivedBid.getItem())) {
						logger.fine("Canceling purchase of "+receivedBid.getItem() +" to "+msg.getSender().getLocalName());
						reply.setContent(Utils.CANCEL);
					}
					else {
						ArrayList<String> sellers;
						if((sellers = liveAuctions.get(receivedBid.getItem())) != null && !sellers.isEmpty()) {
							finished = false;
							reply.setContent(Utils.WAIT);
							send(reply);
							return;
						}
												
						for(Purchase p: purchases) {
							if(p.getItemID().equals(receivedBid.getItem()) &&
									p.getSellerID().equals(msg.getSender().getLocalName())) {
								reply.setContent(Utils.PURCHASE);
								logger.fine("Confirming purchase of "+receivedBid.getItem() +" to "+msg.getSender().getLocalName());
								items.remove(p.getItemID());
								liveAuctions.remove(p.getItemID());
								//Removing myself from DF since i bought item
								//throws a concurrency exception
								//another thread probably using this
								/*Iterator<ServiceDescription> it = dfd.getAllServices();
								while(it.hasNext()) {
									ServiceDescription sd = it.next();
									if(sd.getName().equals(p.getItemID())) {
										System.out.println("REMOVING SD " + sd.getName());
										dfd.removeServices(sd);
									}
								}*/
								
								send(reply);
								return;
							}
						}
						logger.fine("Canceling purchase of "+receivedBid.getItem() +" to "+msg.getSender().getLocalName());
						reply.setContent(Utils.CANCEL);

					}
	                send(reply);
	                return;
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			  
			
				block(); //this block reduces CPU usage from 80% to 8% XD
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
		
		
		private class InformHandler extends Behaviour{
			
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
					SimpleEntry<String, String> content = (SimpleEntry<String, String>)msg.getContentObject();
					if(awaitingRate.containsKey(content.getKey())) {
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

		private int maxTickers;
		private int currentTickers=0;
		
		public CheckEnd(Agent a, long period, int maxTickers) {
			super(a, period);
			this.maxTickers = maxTickers;
		}

		@Override
		protected void onTick() {
			currentTickers++;
			if(items.isEmpty() || currentTickers > maxTickers) {
				logger.fine("AGENT KILLED");
				myAgent.doDelete();
			}
			
		}
		
	}

}