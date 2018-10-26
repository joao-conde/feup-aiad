package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;

public class SellerAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<Bid> bids = new ArrayList<Bid>();
	private Integer currentBidIndex = 0;
	private AID[] buyerAgentsToCurrentItem;

	public void setup() {

		Object[] args = this.getArguments();
		for(Object arg: args) {
			Bid bid = (Bid)arg;
			bids.add(bid);
		}
		
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

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public FetchBuyersBehaviour(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			
			System.out.println("ONTICK");

			// Update the list of seller agents
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("buying");
			sd.setName(getCurrentBid().getItem());
			template.addServices(sd);

			System.out.println(getCurrentBid().getItem());
			
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

		/**
		 * 
		 */
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
				cfp.setContentObject(bids.get(currentBidIndex));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			v.add(cfp);
			
			return v;
		}

		protected void handleAllResponses(Vector responses, Vector acceptances) {
			
			System.out.println("got " + responses.size() + " responses!");
			
			for(int i=0; i<responses.size(); i++) {
				ACLMessage response = ((ACLMessage) responses.get(i));
				Bid receivedBid;
				try {
					
					receivedBid = (Bid) response.getContentObject();
					if(receivedBid.isGreaterThan(getCurrentBid())) {
						updateCurrentBid(receivedBid.getValue(), response.getSender().getLocalName());
						
						ACLMessage msg = response.createReply();
						msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // OR NOT!
						acceptances.add(msg);
						
					} else {
						
						
					}
					
				} catch (UnreadableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				ACLMessage msg = response.createReply();
				msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // OR NOT!
				acceptances.add(msg);
			}
		}
		
		protected void handleAllResultNotifications(Vector resultNotifications) {
			System.out.println("got " + resultNotifications.size() + " result notifs!");
		}
		
	}
	
}
