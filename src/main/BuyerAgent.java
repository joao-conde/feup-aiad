package main;

import java.util.ArrayList;
import java.util.List;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BuyerAgent extends Agent{


	private List<Property> items = new ArrayList<Property>();
	
	protected void setup() {	
		
		System.out.println("Hello! Buyer-agent "+getAID().getName()+" is ready.");
		
		Property prop_1 = new Property("product", "grande_carlos");
		items.add(prop_1);
		Property prop_2 = new Property("product", "grande_vicente");
		items.add(prop_2);
		Property prop_3 = new Property("product", "mequie_luis");
		items.add(prop_3);
		System.out.println(items.get(0));
		
		// Register the book-selling service in the yellow pages
	    DFAgentDescription dfd = new DFAgentDescription();
	    dfd.setName(getAID());
	    ServiceDescription sd = new ServiceDescription();
	    sd.setType("selling");
	    sd.setName("JADE-trading");
	    
	    //Add products as properties
	    for(Property item: items) {
			sd.addProperties(item);
		}
	   
	    dfd.addServices(sd);
	    try {
	      DFService.register(this, dfd);
	    }
	    catch (FIPAException fe) {
	      fe.printStackTrace();
	    }
	}
	
}
