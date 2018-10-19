package main;

import java.util.ArrayList;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class SellerAgent extends Agent{
	
	private List<Property> itemsToSell = new ArrayList<Property>();
	private Property currentItem;
	
	
	public void setup() {
		
		Property prop_1 = new Property("product", "grande_carlos");
		itemsToSell.add(prop_1);
		currentItem = itemsToSell.get(0);
		
		addBehaviour(new TickerBehaviour(this, 10000) {
	        protected void onTick() {
	        	
	          // Update the list of seller agents
	          DFAgentDescription template = new DFAgentDescription();
	          ServiceDescription sd = new ServiceDescription();
	          sd.setType("selling");
	          sd.addProperties(currentItem);
	          template.addServices(sd);   
	          try {
	            DFAgentDescription[] result = DFService.search(myAgent, template);
	            AID[] buyerAgents = new AID[result.length];
	            for (int i = 0; i < result.length; ++i) {
	              buyerAgents[i] = result[i].getName(); 
	            }
	            if(result.length > 1) {
	            	System.out.println(buyerAgents.length);
	            	this.stop();
	            }
	          }
	          catch (FIPAException fe) {
	            fe.printStackTrace();
	          }
	        }
	} );
			
	}
}
