package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

public class SellerAgent extends Agent {

	private ArrayList<String> itemsToSell = new ArrayList<String>();
	private String currentItem;
	private AID[] buyerAgents;

	public void setup() {

		Object[] args = this.getArguments();
		for(Object arg: args) {
			itemsToSell.add(arg.toString());
		}
		
		currentItem = itemsToSell.get(0);
		
		SequentialBehaviour fetchAndPropose = new SequentialBehaviour();
		FetchBuyersBehaviour b1 = new FetchBuyersBehaviour(this, 3000);
		FIPAContractNetInit b2 = new FIPAContractNetInit(this,  new ACLMessage(ACLMessage.CFP));
		fetchAndPropose.addSubBehaviour(b1);
		fetchAndPropose.addSubBehaviour(b2);
		
		addBehaviour(fetchAndPropose);

	}

	private class FetchBuyersBehaviour extends TickerBehaviour {

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
			sd.setName(currentItem);
			template.addServices(sd);

			System.out.println(currentItem);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				buyerAgents = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					buyerAgents[i] = result[i].getName();
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

		public FIPAContractNetInit(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		protected Vector prepareCfps(ACLMessage cfp) {
			Vector v = new Vector();
			
			for(AID aid: buyerAgents) {
				cfp.addReceiver(aid);
			}
			cfp.setContent("this is a call...");
			
			v.add(cfp);
			
			return v;
		}

		protected void handleAllResponses(Vector responses, Vector acceptances) {
			
			System.out.println("got " + responses.size() + " responses!");
			
			for(int i=0; i<responses.size(); i++) {
				ACLMessage msg = ((ACLMessage) responses.get(i)).createReply();
				msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // OR NOT!
				acceptances.add(msg);
			}
		}
		
		protected void handleAllResultNotifications(Vector resultNotifications) {
			System.out.println("got " + resultNotifications.size() + " result notifs!");
		}
		
	}
	
}
