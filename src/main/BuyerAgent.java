package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetResponder;

public class BuyerAgent extends Agent{


	private ArrayList<String> items = new ArrayList<String>();
	//private HashMap<AID, Integer> sellersOpinion = new HashMap<AID,Integer>();
	
	protected void setup() {	
		
		Object[] args = this.getArguments();
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		
		System.out.println("Hello! Im buyer-agent " +getAID().getLocalName()+" is ready.");
		for(Object arg: args) {
			String argument = (String) arg;
			items.add(argument);
			
			ServiceDescription sd = new ServiceDescription();
			sd.setType("buying");
			sd.setName(argument);
			dfd.addServices(sd);
			System.out.println(argument);
		}
		
	    try {
	      DFService.register(this, dfd);
	    }
	    catch (FIPAException fe) {
	      fe.printStackTrace();
	    }
	    
	    addBehaviour(new FIPAContractNetResp(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
	}
	
	private class FIPAContractNetResp extends ContractNetResponder {

		private static final long serialVersionUID = 1L;

		public FIPAContractNetResp(Agent a, MessageTemplate mt) {
			super(a, mt);
		}
		
		
		protected ACLMessage handleCfp(ACLMessage cfp) {
			
			ACLMessage reply = cfp.createReply();
			reply.setPerformative(ACLMessage.PROPOSE);
			
			try {
				Bid receivedBid = (Bid) cfp.getContentObject();
				receivedBid.setNewValue(receivedBid.getValue()+1);
				reply.setContentObject(receivedBid);
				
			} catch (UnreadableException | IOException e) {
				e.printStackTrace();
			}

			return reply;
		}
		
		protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
			System.out.println(myAgent.getLocalName() + " got a reject...");
		}

		protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
			System.out.println(myAgent.getLocalName() + " got an accept!");
			ACLMessage result = accept.createReply();
			result.setPerformative(ACLMessage.INFORM);
			result.setContent("this is the result");
			
			return result;
		}

	}
	
	
}