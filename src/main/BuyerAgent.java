package main;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

import main.Purchase;
import utilities.Utils;

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
import jade.util.Logger;

public class BuyerAgent extends Agent{


	private static final long serialVersionUID = 1L;
	private Logger logger;
	private String agentName;
	private HashMap<String,Float> items = new HashMap<String,Float>(); //itemID, maxValue
	private HashMap<String,Integer> attempts = new HashMap<String,Integer>(); //itemID, attempts
	private HashMap<String,Float> ratings = new HashMap<String,Float>(); //sellerID, averageRating
	private ArrayList<Purchase> purchases = new ArrayList<Purchase>(); 
	
	private float computeAverageRating(String sellerID) {
		float sumRatings = 0, sellerPurchases = 0, average;
		for(Purchase p: purchases) {
			if(p.getSellerID().equals(sellerID)) {
				sellerPurchases++;
				sumRatings += p.getRating();
			}
		}
		return (float) (sellerPurchases == 0 ? 0.8 : sumRatings/sellerPurchases);
	}
	
	protected void setup() {	
		
		Object[] args = this.getArguments();
		DFAgentDescription dfd = new DFAgentDescription();
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
					
					if(!items.containsKey(receivedBid.getItem())) {
						reply.setPerformative(ACLMessage.REFUSE);
						return reply;
					}
					
					if(cfp.getProtocol() == Utils.CFP_PROTOCOL) {
						String sellerID = cfp.getSender().getLocalName();
						ratings.put(sellerID, computeAverageRating(sellerID));
					}
					
					logger.fine(agentName+": Received a CFP from "+cfp.getSender().getLocalName()+" to "+receivedBid.getItem());
					
					String lastBidderName=null;
					reply.setPerformative(ACLMessage.PROPOSE);
					
					if((lastBidderName = receivedBid.getLastBidder()) != null) {
						
						//This agent is not the currrent winner
						logger.fine(agentName+": LastHighest was "+lastBidderName + " at " + receivedBid.getValue());
						if(!lastBidderName.equals(agentName)) {
							
							logger.fine(receivedBid.getItem() + " ITEM RATE OF SELLER " + ratings.get(cfp.getSender().getLocalName()));
							if(receivedBid.getValue() + receivedBid.getMinIncrease() <= items.get(receivedBid.getItem()) * ratings.get(cfp.getSender().getLocalName()))
								receivedBid.setNewValue(Utils.round(receivedBid.getValue()+receivedBid.getMinIncrease(), 3));
							else
								reply.setPerformative(ACLMessage.REFUSE);
						}
						
					} else {
						//first bid (cfp call)
						if(receivedBid.getValue() > items.get(receivedBid.getItem()) * ratings.get(cfp.getSender().getLocalName()))
							reply.setPerformative(ACLMessage.REFUSE); //first value is already too high
					}
					reply.setContentObject(receivedBid);
					logger.fine(agentName+": Propose to "+receivedBid.getItem()+" with "+reply.getPerformative()+" and value "+ receivedBid.getValue());
					
				} catch (UnreadableException | IOException e) {
					e.printStackTrace();
				}
				
				return reply;
			}
			
/*			protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
				logger.fine(myAgent.getLocalName() + " got a reject...");
			}*/

			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
				try {
					Bid bid = (Bid)accept.getContentObject();
					Purchase purchase = new Purchase(bid, accept.getSender().getLocalName(), Integer.parseInt(accept.getUserDefinedParameter("DeliveryTime")), items.get(bid.getItem()));
					computeAverageRating(accept.getSender().getLocalName());
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
	
	
	
	public class Confirmation extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		
		MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);    
		ACLMessage reply;

		@Override
		public void action() {
			ACLMessage msg = receive( template );
			
			if (msg!=null) {
				reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				reply.addReceiver(msg.getSender());
				
				try {
					Bid receivedBid = (Bid) msg.getContentObject();
					if(!items.containsKey(receivedBid.getItem())) {
						reply.setContent(Utils.CANCEL);
					}
					else {
						
						for(Purchase p: purchases) {
							if(p.getItemID().equals(receivedBid.getItem()) &&
									p.getSellerID().equals(msg.getSender().getLocalName())) {
								reply.setContent(Utils.PURCHASE);
								items.remove(p.getItemID());
								
								send(reply);
								return;
							}
						}
						reply.setContent(Utils.CANCEL);	
		                send(reply);

					}
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			  
			}
			block(); //this block reduces CPU usage from 80% to 8% XD
		}
	}	
	
}
