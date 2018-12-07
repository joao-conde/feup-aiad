package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;
import jade.util.Logger;
import utilities.MarketLogger;
import utilities.SellerStatistics;
import utilities.Utils;

public class SellerAgent extends Agent {

	private static final long serialVersionUID = 7888357163660384182L;
	private Logger logger;
	private SellerStatistics statManager;
	private ArrayList<Bid> bids = new ArrayList<Bid>();
	private Integer extraDelay, currentBidIndex = 0;
	private AID[] buyerAgentsToCurrentItem;
	private Bid highestBid = null;
	private String agentName;
	
	private ArrayList<Bid> itemsSold = new ArrayList<Bid>();

	public void setup() {

		Object[] args = this.getArguments();

		extraDelay = Integer.parseInt(args[0].toString());

		for (int i = 1; i < args.length; i++) {
			Bid bid = (Bid) args[i];
			bids.add(bid);
		}

		agentName = this.getLocalName();

		statManager = new SellerStatistics(agentName);
		//logger = MarketLogger.createLogger(this.getClass().getName(), agentName);

		addBehaviour(new MainBehaviour(this));
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
			
			message.setContentObject(itemsSold);

			this.send(message);
		} catch (FIPAException | IOException fe) {
			fe.printStackTrace();
		}

	}

	public Bid getCurrentBid() {
		return bids.get(currentBidIndex);
	}

	public void updateCurrentBid(float value, String bidder) {
		Bid bid = getCurrentBid();
		bid.setNewValue(value);
		bid.setLastBidder(bidder);
	}

	private class MainBehaviour extends SimpleBehaviour {

		private static final long serialVersionUID = 1L;
		private boolean finished = false;

		public MainBehaviour(Agent a) {
			super(a);

		}

		@Override
		public void action() {
			if (bids.isEmpty()) {
				//logger.fine(this.myAgent + " has no more items to sell");
				finished = true;
				return;
			}

			SequentialBehaviour fetchAndPropose = new SequentialBehaviour();
			FetchBuyersBehaviour b1 = new FetchBuyersBehaviour(myAgent, 2000);
			FIPAContractNetInit b2 = new FIPAContractNetInit(myAgent, new ACLMessage(ACLMessage.CFP));
			fetchAndPropose.addSubBehaviour(b1);
			fetchAndPropose.addSubBehaviour(b2);
			myAgent.addBehaviour(fetchAndPropose);
			finished = true;
		}

		@Override
		public boolean done() {
			return finished;
		}

	}

	private class FetchBuyersBehaviour extends TickerBehaviour {

		private static final long serialVersionUID = 1L;

		private int attempts = 0;

		public FetchBuyersBehaviour(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {

			// Update the list of buyer agents
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(Utils.SD_BUY);
			sd.setName(getCurrentBid().getItem());
			template.addServices(sd);

			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				buyerAgentsToCurrentItem = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					buyerAgentsToCurrentItem[i] = result[i].getName();
				}

				if (result.length <= 1 && attempts < 2) {
					attempts++;
				} else if (result.length <= 1 && attempts == 2) {
					currentBidIndex++;
					currentBidIndex %= bids.size();
					this.reset();
				} else {
					this.stop();
				}

			} catch (FIPAException fe) {
				fe.printStackTrace();
			}

		}

	}

	private class FIPAContractNetInit extends ContractNetInitiator {

		private static final long serialVersionUID = 1L;

		public FIPAContractNetInit(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		protected Vector<ACLMessage> prepareCfps(ACLMessage cfp) {

			Vector<ACLMessage> v = new Vector<ACLMessage>();

			for (AID aid : buyerAgentsToCurrentItem) {
				cfp.addReceiver(aid);
			}

			try {
				Bid bid = bids.get(currentBidIndex);
				cfp.setContentObject(bid);
				cfp.setProtocol(Utils.CFP_PROTOCOL);
			} catch (IOException e) {
				e.printStackTrace();
			}

			v.add(cfp);

			// e.g. Seller2: started an auction of bananas
			/*logger.fine("--------------AUCTION--------------");
			logger.fine(agentName + " started an auction of " + bids.get(currentBidIndex).getItem());*/

			return v;
		}

		protected void handleAllResponses(Vector responses, Vector acceptances) {

			/*logger.fine(agentName + " got " + responses.size() + " responses to " + bids.get(currentBidIndex).getItem()
					+ " auction\n");
			logger.fine("--------------New iteration--------------");*/
			for (int i = 0; i < responses.size(); i++) {
				ACLMessage response = ((ACLMessage) responses.get(i));
				Bid receivedBid;
				try {
					if (response.getPerformative() == ACLMessage.REFUSE) {
						//logger.fine(agentName + ": Received REFUSE from " + response.getSender().getLocalName());
						continue;

					} else if (response.getPerformative() == ACLMessage.PROPOSE) {

						receivedBid = (Bid) response.getContentObject();

						/*statManager.incItemPropose(receivedBid.getItem());
						logger.fine(agentName + ": " + response.getSender().getLocalName() + " proposes "
								+ receivedBid.getValue() + " to " + receivedBid.getItem());*/
						if (highestBid != null) {
							if (receivedBid.getValue() > highestBid.getValue()) {
								highestBid = receivedBid;
								highestBid.setLastBidder(response.getSender().getLocalName());
								/*logger.fine("HighestBidder: " + highestBid.getLastBidder() + " with value: "
										+ highestBid.getValue());*/
							}
						} else {
							highestBid = receivedBid;
							highestBid.setLastBidder(response.getSender().getLocalName());
							/*logger.fine("HighestBidder: " + highestBid.getLastBidder() + " with value: "
									+ highestBid.getValue());*/
						}
					}

				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			}

			for (int i = 0; i < responses.size(); i++) {
				ACLMessage response = ((ACLMessage) responses.get(i));
				try {
					if (response.getPerformative() == ACLMessage.REFUSE)
						continue;

					else if (response.getPerformative() == ACLMessage.PROPOSE) {
						ACLMessage msg = response.createReply();
						msg.setPerformative(ACLMessage.CFP);
						msg.setContentObject(highestBid);
						msg.setProtocol(null);
						acceptances.add(msg);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (acceptances.size() == 1) {
				ACLMessage winnerMessage = (ACLMessage) acceptances.get(0);

				Bid bid;
				try {
					bid = (Bid) winnerMessage.getContentObject();
					winnerMessage.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

					if (extraDelay == 0)
						winnerMessage.addUserDefinedParameter("DeliveryTime",
								(computeDelay(1, bid.getDeliveryTime())).toString());
					else
						winnerMessage.addUserDefinedParameter("DeliveryTime",
								(computeDelay(bid.getDeliveryTime(), bid.getDeliveryTime() + extraDelay)).toString());

					BidKeeper keeper = new BidKeeper(myAgent, 1000);
					addBehaviour(keeper);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			} else if (acceptances.size() != 0) {
				newIteration(acceptances);
			} else {
				//logger.fine("No one bought, moving to next item");
				currentBidIndex++;
				currentBidIndex %= bids.size();
			}
		}

		protected void handleAllResultNotifications(Vector resultNotifications) {
			/*logger.fine(agentName + " got " + resultNotifications.size() + " responses to "
					+ bids.get(currentBidIndex).getItem() + " auction\n");*/
		}

		private Integer computeDelay(int min, int max) {
			if (max == min)
				return min;
			Random r = new Random(System.currentTimeMillis());
			return r.nextInt(max - min) + min;
		}

		private class BidKeeper extends WakerBehaviour {

			private static final long serialVersionUID = 1L;

			public BidKeeper(Agent a, long timeout) {
				super(a, timeout);
			}

			@Override
			public void onWake() {
				if (highestBid != null) {
					AID buyer = getAID(highestBid.getLastBidder());
					ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
					message.setSender(getAID());
					message.addReceiver(buyer);

					try {
						message.setContentObject(highestBid);
					} catch (IOException e) {
						e.printStackTrace();
					}
					myAgent.send(message);
					/*logger.fine("Trying to confirm purchase of " + highestBid.getItem() + " to "
							+ highestBid.getLastBidder());*/
					addBehaviour(new HandleInform());
				} else {
					System.err.println("Error checking ended auction status");
				}
			}

			private class HandleInform extends CyclicBehaviour {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public void action() {
					MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
					ACLMessage msg = receive(template);
					if (msg != null) {
						if (msg.getContent().equals(Utils.WAIT)) {
							//logger.fine("Waiting confirmation, requested by buyer " + msg.getSender().getLocalName());
							super.reset();
						} else {
							if (msg.getContent().equals(Utils.PURCHASE)) {

								itemsSold.add(highestBid);
								/*statManager.addItemSell(highestBid);//TODO comment all stats and loggers, possible decrease timers ---> faster run
								logger.fine(highestBid.getItem() + " sold to " + msg.getSender().getLocalName());*/

								if (!bids.isEmpty()) {
									bids.remove(bids.get(currentBidIndex));

									if (currentBidIndex >= bids.size())
										currentBidIndex = bids.size() - 1;
								}
							} else if (msg.getContent().equals(Utils.CANCEL)) {
								/*statManager.incPurchaseCancels();
								logger.fine("Purchase of " + highestBid.getItem() + " canceled by buyer "
										+ msg.getSender().getLocalName());*/
								currentBidIndex++;
								currentBidIndex %= bids.size();
							}
							onEnd();
						}

					} else
						this.block();

				}

				public int onEnd() {
					if (!bids.isEmpty()) {
						this.myAgent.addBehaviour(new MainBehaviour(myAgent));
						highestBid = null;
						//logger.fine("Moving to new auction");
					} else {
						//logger.fine(myAgent.getLocalName() + " has nothing else to sell - TERMINATED");
						//statManager.logStatistics(logger);
						informSimulatorAgent();
						myAgent.doDelete();
					}
					return 0;
				}

			}

		}

	}
}
