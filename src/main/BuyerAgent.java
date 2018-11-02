package main;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;

import communication.MessageType;


import java.util.HashMap;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SSIteratedContractNetResponder;
import jade.proto.SSResponderDispatcher;

public class BuyerAgent extends Agent{


	private static final long serialVersionUID = 1L;
	private HashMap<String,Float> items = new HashMap<String,Float>(); //itemID, maxValue
	private String agentName;
	private HashMap<Bid, String> purchases = new HashMap<Bid, String>(); //Bid, Seller name
	
	protected void setup() {	
		
		Object[] args = this.getArguments();
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		
		agentName = this.getLocalName();
		
		for(Object arg: args) {
			SimpleEntry<String,Float> argument = (SimpleEntry<String,Float>) arg;
			items.put(argument.getKey(), argument.getValue());
			
			ServiceDescription sd = new ServiceDescription();
			sd.setType(MessageType.SD_BUY);
			sd.setName(argument.getKey());
			dfd.addServices(sd);
		}
		
	    try {
	      DFService.register(this, dfd);
	    }
	    catch (FIPAException fe) {
	      fe.printStackTrace();
	    }
	    
	    addBehaviour(new CFPDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
	    addBehaviour(new Confirmation());
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
					
					System.out.println(agentName+": Received a CFP from "+cfp.getSender().getLocalName()+" to "+receivedBid.getItem());
					
					String lastBidderName=null;
					reply.setPerformative(ACLMessage.PROPOSE);
					
					if((lastBidderName = receivedBid.getLastBidder()) != null) {
						
						//This agent is not the currrent winner
						System.out.println(agentName+": LastHighest was "+lastBidderName);
						if(!lastBidderName.equals(agentName)) {
							
							if(receivedBid.getValue() + receivedBid.getMinIncrease() <= items.get(receivedBid.getItem()))
								receivedBid.setNewValue(round(receivedBid.getValue()+receivedBid.getMinIncrease(), 3));
							else
								reply.setPerformative(ACLMessage.REFUSE);
						}
						
					} else {
						//first bid (cfp call)
						if(receivedBid.getValue() > items.get(receivedBid.getItem()))
							reply.setPerformative(ACLMessage.REFUSE); //first value is already too high
					}
					reply.setContentObject(receivedBid);
					System.out.println(agentName+": Propose to "+receivedBid.getItem()+" with "+reply.getPerformative()+" and value "+ receivedBid.getValue());
					
				} catch (UnreadableException | IOException e) {
					e.printStackTrace();
				}
				
				return reply;
			}
			
			protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
				System.out.println(myAgent.getLocalName() + " got a reject...");
			}

			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
				
				Bid bid = (Bid)accept.getContentObject();
				if(purchases.containsKey(bid)) {
					for(Bid b: purchases.keySet()) {
						if(b.equals(bid)) {
							//if(b.g)
							
							break;
						}
					}
					
					//purchases.remove(bid);
					//purchases.put(bid, accept.getSender().getLocalName());
				}
				
				
				return null;
			}
			
		}

	}
	
	public static float round(float number, int scale) {
	    int pow = 10;
	    for (int i = 1; i < scale; i++)
	        pow *= 10;
	    float tmp = number * pow;
	    return ( (float) ( (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) ) ) / pow;
	}
	
	public class Confirmation extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		
		MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);    
		ACLMessage reply;

		@Override
		public void action() {
			ACLMessage msg = receive( template );

			if (msg!=null) {
			    // reply informing whether is is to confirm the auction winning or not
                reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.addReceiver(msg.getSender());
                try {
					Bid bid = (Bid) msg.getContentObject();
					//TODO: add bid to purchases if none there
					
					if(purchases.containsKey(bid)) {
						//if(purchases.get(bid) != msg.getSender().getLocalName()) {
							System.out.println("Different bid then the one i have saved");
							//how to get the bid if it is the key?
							//possible to do keySet() and iterate but it is
							//ineffective. better solution TODO
							//get a map of <string, bid> where string is
							//a combined string of sellerName-Item
							//this will be unique
							//allows us to get the bid for each seller-item
						}
					}
					
					if(items.containsKey(bid.getItem())) {						
						items.remove(bid.getItem());
						System.out.println("-->Acquired " + bid.getItem() + " for " + bid.getValue() + " from " + msg.getSender().getLocalName());
						reply.setContent(MessageType.PURCHASE);
					}
					else {
						reply.setContent(MessageType.CANCEL);
					}
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
                System.out.println(reply);
                send(reply);
			}
		
		}
	}	
	
}
