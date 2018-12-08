package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
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
	private ArrayList<Integer> bidsCounter = new ArrayList<Integer>();
	private Integer extraDelay, currentBidIndex = 0;
	private AID[] buyerAgentsToCurrentItem;
	private Bid highestBid = null;
	private String agentName;
	private SequentialBehaviour fetchAndPropose;
	private boolean canTerminate = false;
	
	private ArrayList<Bid> itemsSold = new ArrayList<Bid>();

	public void setup() {

		Object[] args = this.getArguments();

		extraDelay = Integer.parseInt(args[0].toString());

		for (int i = 1; i < args.length; i++) {
			Bid bid = (Bid) args[i];
			bids.add(bid);
			bidsCounter.add(0);
		}

		agentName = this.getLocalName();

		statManager = new SellerStatistics(agentName);
		logger = MarketLogger.createLogger(this.getClass().getName(), agentName);

		fetchAndPropose = new SequentialBehaviour() {

			private static final long serialVersionUID = 1L;

			public int onEnd() {
				logger.fine("ON END");
			    reset();
			    myAgent.addBehaviour(this);
			    return super.onEnd();
			}
		};
		logger.fine("STARTTIIIING");
		MainBehaviour m1 = new MainBehaviour(this);
		FetchBuyersBehaviour b1 = new FetchBuyersBehaviour(this, 2000);
		FIPAContractNetInit b2 = new FIPAContractNetInit(this, new ACLMessage(ACLMessage.CFP));
		BidKeeper keeper = new BidKeeper(this, 1000);
		fetchAndPropose.addSubBehaviour(m1);
		fetchAndPropose.addSubBehaviour(b1);
		fetchAndPropose.addSubBehaviour(b2);
		fetchAndPropose.addSubBehaviour(keeper);
		fetchAndPropose.addSubBehaviour(new HandleInform());
		this.addBehaviour(fetchAndPropose);
		
		
	}
	
	protected void takeDown() {
		//informSimulatorAgent();
		this.doDelete();
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
			logger.fine("Starting seller");
			logger.fine(""+bidsCounter);
			if (bids.isEmpty() || (!bidsCounter.contains(0) && !bidsCounter.contains(1))) {
				logger.fine(this.myAgent + " has no more items to sell");
				statManager.logStatistics(logger);
				takeDown();
				finished = false;
			} else {
				highestBid = null;
				finished = true;
			}
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
			logger.fine("FETCHINGGGG "+ getCurrentBid().getItem() + "index "+currentBidIndex);
			// Update the list of buyer agents
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(Utils.SD_BUY);
			sd.setName(getCurrentBid().getItem());
			template.addServices(sd);

			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				buyerAgentsToCurrentItem = new AID[result.length];
				logger.fine("FOUND "+result.length);
				for (int i = 0; i < result.length; ++i) {
					buyerAgentsToCurrentItem[i] = result[i].getName();
				}

				if (result.length <= 1 && attempts < 2) {
					attempts++;
				} else if (result.length <= 1 && attempts == 2) {
					bidsCounter.set(currentBidIndex,bidsCounter.get(currentBidIndex)+1);
					currentBidIndex++;
					currentBidIndex %= bids.size();
					fetchAndPropose.reset();
				} else {
					bidsCounter.set(currentBidIndex,bidsCounter.get(currentBidIndex)+1);
					this.stop();
				}

			} catch (FIPAException fe) {
				fe.printStackTrace();
			}

		}
		public int onEnd() {
			logger.fine("fetch end");
			return 0;
		}

	}

	private class FIPAContractNetInit extends ContractNetInitiator {

		private static final long serialVersionUID = 1L;

		public FIPAContractNetInit(Agent a, ACLMessage msg) {
			super(a, msg);
		}
		
		public int onEnd() {
			logger.fine("FINISHING FIPA");
			return 0;
		}

		protected Vector<ACLMessage> prepareCfps(ACLMessage cfp) {
			

			Vector<ACLMessage> v = new Vector<ACLMessage>();
			for(int i=0; i< buyerAgentsToCurrentItem.length; i++) {
				logger.fine("Buyer "+buyerAgentsToCurrentItem[i].getLocalName());
			}
			cfp = new ACLMessage(ACLMessage.CFP);
			
			for (AID aid : buyerAgentsToCurrentItem) {
				logger.fine(""+aid.getLocalName());
				cfp.addReceiver(aid);
			}
			
			logger.fine("PREPARE CFP ");

			try {
				Bid bid = bids.get(currentBidIndex);
				cfp.setContentObject(bid);
				cfp.setProtocol(Utils.CFP_PROTOCOL);
			} catch (IOException e) {
				e.printStackTrace();
			}

			v.add(cfp);

			// e.g. Seller2: started an auction of bananas
			logger.fine("--------------AUCTION--------------");
			logger.fine(agentName + " started an auction of " + bids.get(currentBidIndex).getItem());

			return v;
		}

		protected void handleAllResponses(Vector responses, Vector acceptances) {

			logger.fine("--------------New iteration--------------");
			for (int i = 0; i < responses.size(); i++) {
				ACLMessage response = ((ACLMessage) responses.get(i));
				Bid receivedBid;
				try {
					if (response.getPerformative() == ACLMessage.REFUSE) {
						logger.fine(agentName + ": Received REFUSE from " + response.getSender().getLocalName());
						continue;

					} else if (response.getPerformative() == ACLMessage.PROPOSE) {

						receivedBid = (Bid) response.getContentObject();

						statManager.incItemPropose(receivedBid.getItem());
						logger.fine(agentName + ": " + response.getSender().getLocalName() + " proposes "
								+ receivedBid.getValue() + " to " + receivedBid.getItem());
						if (highestBid != null) {
							if (receivedBid.getValue() > highestBid.getValue()) {
								highestBid = receivedBid;
								highestBid.setLastBidder(response.getSender().getLocalName());
								logger.fine("HighestBidder: " + highestBid.getLastBidder() + " with value: "
										+ highestBid.getValue());
							}
						} else {
							highestBid = receivedBid;
							highestBid.setLastBidder(response.getSender().getLocalName());
							logger.fine("HighestBidder: " + highestBid.getLastBidder() + " with value: "
									+ highestBid.getValue());
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
			logger.fine(agentName + " got " + resultNotifications.size() + " responses to "
					+ bids.get(currentBidIndex).getItem() + " auction kkkkfnjf\n");
		}

		private Integer computeDelay(int min, int max) {
			if (max == min)
				return min;
			Random r = new Random(System.currentTimeMillis());
			return r.nextInt(max - min) + min;
		}

	}
	
	private class BidKeeper extends WakerBehaviour {

		private static final long serialVersionUID = 1L;

		public BidKeeper(Agent a, long timeout) {
			super(a, timeout);
		}

		@Override
		public void onWake() {
			logger.fine("BID KEEPER");
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
				logger.fine("Trying to confirm purchase of " + highestBid.getItem() + " to "
						+ highestBid.getLastBidder());
			} else {
				System.err.println("Error checking ended auction status");
			}
		}

		public int onEnd() {
			logger.fine("FINISHING WAKER");
			return 0;
		}

	}
	
	private class HandleInform extends SimpleBehaviour {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private boolean finished1 = false;

		@Override
		public void action() {
			MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = receive(template);
			if (msg != null) {
				if (msg.getContent().equals(Utils.WAIT)) {
					logger.fine("Waiting confirmation, requested by buyer " + msg.getSender().getLocalName());
					super.reset();
				} else {
					if (msg.getContent().equals(Utils.PURCHASE)) {

						itemsSold.add(highestBid);
						statManager.addItemSell(highestBid);//TODO comment all stats and loggers, possible decrease timers ---> faster run
						logger.fine(highestBid.getItem() + " sold to " + msg.getSender().getLocalName());

						if (!bids.isEmpty()) {
							for(int i=0; i< bids.size(); i++) {
								logger.fine("ELEMENT: "+bids.get(i).getItem());
							}
							logger.fine("INDEX: "+currentBidIndex);
							int bidIndex = (int)currentBidIndex;
							bidsCounter.remove(bidIndex);
							logger.fine("SHOULD REMOVE "+bids.get(0));
							bids.remove(bidIndex);
							logger.fine("BIDS: "+bids);

							if (currentBidIndex >= bids.size())
								currentBidIndex = bids.size() - 1;
						}
					} else if (msg.getContent().equals(Utils.CANCEL)) {
						statManager.incPurchaseCancels();
						logger.fine("Purchase of " + highestBid.getItem() + " canceled by buyer "
								+ msg.getSender().getLocalName());
						currentBidIndex++;
						currentBidIndex %= bids.size();
					}
				}
				finished1 = true;
			} else 
				finished1 = false;

		}

		public boolean done() {
			logger.fine("INFORM RECEIVED");
			logger.fine("COUNTER: "+bidsCounter);
			logger.fine("INDEX22: "+currentBidIndex);
			return finished1;
		}

	}
}
