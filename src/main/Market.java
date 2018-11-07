package main;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;

import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import utilities.Utils;

public class Market { 
		
	private Runtime jadeRt; //JADE runtime
	private ContainerController mainContainer; //Main JADE container
	 
	public static void main(String[] args) {
		new Market();
	}
	
	public Market() {
		try {
			new File(Utils.LOG_PATH).mkdirs();
			this.initializeContainers();
			this.initializeAgents();
		} catch (SecurityException | StaleProxyException e) {
			e.printStackTrace();
		}	
				
	}
		
	public void initializeContainers() {
		this.jadeRt = Runtime.instance();
		this.mainContainer = jadeRt.createMainContainer(new ProfileImpl());
	}
	
	public void initializeAgents() throws StaleProxyException {

		
		Object[] b1 = {new SimpleEntry<String,Float>("batatas",(float)15.0)},
			     b2 = {new SimpleEntry<String,Float>("batatas", (float) 15.5),new SimpleEntry<String,Float>("bananas",(float)17)},
			     b3 = {new SimpleEntry<String,Float>("batatas", (float) 17.00)};
		
		//create new buyer agents
		AgentController buyer = this.mainContainer.createNewAgent("Carlos", "main.BuyerAgent", b1),
						buyer2 = this.mainContainer.createNewAgent("Toy", "main.BuyerAgent", b2),
						buyer3 = this.mainContainer.createNewAgent("buyerAgent3", "main.BuyerAgent", b3);
		
		//start new buyer agents 
		buyer.start();
		buyer2.start();
		buyer3.start();
		
		Object[] s1 = {0, new Bid("batatas", (float)10.00, 5,(float)0.5),new Bid("bananas",(float)12,6,(float)0.4)},
			  s2 = {0, new Bid("batatas",(float)9.00, 6,(float)0.5)};
		
		//create new seller agents
		AgentController seller = this.mainContainer.createNewAgent("Antonio", "main.SellerAgent", s1),
						seller2 = this.mainContainer.createNewAgent("HLC", "main.SellerAgent", s2);
		
		//start new sellers
		seller.start();
		seller2.start();

	}


}
