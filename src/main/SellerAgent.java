package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;

public class SellerAgent extends Agent {


	private static final long serialVersionUID = 7888357163660384182L;
	private ArrayList<Bid> bids = new ArrayList<Bid>();
	private Integer currentBidIndex = 0;
	private AID[] buyerAgentsToCurrentItem;
	private Bid highestBid = null;
	private String agentName;
	

	public void setup() {

		Object[] args = this.getArguments();
		for(Object arg: args) {
			Bid bid = (Bid)arg;
			bids.add(bid);
		}
		
		agentName = this.getLocalName();
		
		SequentialBehaviour fetchAndPropose = new SequentialBehaviour();
		FetchBuyersBehaviour b1 = new FetchBuyersBehaviour(this, 3000);
		FIPAContractNetInit b2 = new FIPAContractNetInit(this,  new ACLMessage(ACLMessage.CFP));
		fetchAndPropose.addSubBehaviour(b1);
		fetchAndPropose.addSubBehaviour(b2);
		
		addBehaviour(fetchAndPropose);

	}
	
	public Bid getCurrentBid() {
		return bids.get(currentBidIndex);
	}
	
	public void updateCurrentBid(float value, String bidder) {
		Bid bid = getCurrentBid();
		bid.setNewValue(value);
		bid.setLastBidder(bidder);
	}
	
 
	private class FetchBuyersBehaviour extends TickerBehaviour {


		private static final long serialVersionUID = 1L;

		public FetchBuyersBehaviour(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			

			// Update the list of seller agents
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("buying");
			sd.setName(getCurrentBid().getItem());
			template.addServices(sd);

			
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				buyerAgentsToCurrentItem = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					buyerAgentsToCurrentItem[i] = result[i].getName();
				}
				
				if (result.length > 1) {
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
			
			for(AID aid: buyerAgentsToCurrentItem) {
				cfp.addReceiver(aid);
			}
			
			try {
				Bid bid = bids.get(currentBidIndex);
				cfp.setContentObject(bid);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			v.add(cfp);
			
			//e.g. Seller2: started an auction of bananas
			System.out.println(agentName+": started an auction of "+bids.get(currentBidIndex).getItem());
			
			return v;
		}

		protected void handleAllResponses(Vector responses, Vector acceptances) {
			
			System.out.println("got " + responses.size() + " responses!");
			
			for(int i=0; i<responses.size(); i++) {
				ACLMessage response = ((ACLMessage) responses.get(i));
				Bid receivedBid;
				try {
					if(response.getPerformative() == ACLMessage.REFUSE) {
						System.out.println(agentName+": Received "+ACLMessage.REFUSE+" from "+response.getSender().getLocalName());
						continue;
					
					} else if(response.getPerformative() == ACLMessage.PROPOSE){
						receivedBid = (Bid) response.getContentObject();
						System.out.println(agentName+": "+response.getSender().getLocalName()+" proposes "+ receivedBid.getValue() + " to " + receivedBid.getItem());
						if(highestBid != null) {
							
							if(receivedBid.getValue() > highestBid.getValue()) {
								highestBid = receivedBid;
								highestBid.setLastBidder(response.getSender().getLocalName());
								System.out.println("HighestBidder: "+highestBid.getLastBidder()+ " with value: "+highestBid.getValue());
							}
							
						} else {
							highestBid = receivedBid;
							highestBid.setLastBidder(response.getSender().getLocalName());
							System.out.println("HighestBidder: "+highestBid.getLastBidder()+ " with value: "+highestBid.getValue());
						}
					}
						
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			}
			
			for(int i=0; i<responses.size(); i++) {
				ACLMessage response = ((ACLMessage) responses.get(i));
				try {
					if(response.getPerformative() == ACLMessage.REFUSE)
						continue;
					
					else if(response.getPerformative() == ACLMessage.PROPOSE){
						
						ACLMessage msg = response.createReply();
						msg.setPerformative(ACLMessage.CFP);
						msg.setContentObject(highestBid);
						acceptances.add(msg);
						
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			if(acceptances.size() == 1) {
				ACLMessage winnerMessage = (ACLMessage) acceptances.get(0);
				winnerMessage.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
				BidKeeper keeper = new BidKeeper(myAgent, 3000);
				addBehaviour(keeper);
			} else {
				newIteration(acceptances);
			}
		}
		
		protected void handleAllResultNotifications(Vector resultNotifications) {
			System.out.println("got " + resultNotifications.size() + " result notifs!");
		}
		
		private class BidKeeper extends WakerBehaviour{

			private static final long serialVersionUID = 1L;

			public BidKeeper(Agent a, long timeout) {
				super(a, timeout);
			}
			
			@Override
			public void onWake() {
				
				if(highestBid != null) {
					AID teste = getAID(highestBid.getLastBidder());
					ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
					message.setSender(getAID());
					message.addReceiver(teste);
					myAgent.send(message);
				} else {
					System.err.println("Error checking ended auction status");
				}
				System.out.println("Ola");
			}	
		}
		
	}
	
	
}
