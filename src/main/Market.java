package main;

import java.util.AbstractMap.SimpleEntry;

import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Market {
	
	private Runtime jadeRt; //JADE runtime
	private ContainerController mainContainer; //Main JADE container
	 
	public static void main(String[] args) {
		new Market();
	}
	
	public Market() {
		this.initializeContainers();
		
		try {
			this.initializeAgents();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}
	
	public void initializeContainers() {
		this.jadeRt = Runtime.instance();
		this.mainContainer = jadeRt.createMainContainer(new ProfileImpl());
	}
	
	public void initializeAgents() throws StaleProxyException {

		
		Object[] b1 = {new SimpleEntry<String,Float>("banana",(float)11.20), new SimpleEntry<String,Float>("pessego",(float)12.00)},
			   b2 = {new SimpleEntry<String,Float>("banana", (float) 10.10), new SimpleEntry<String,Float>("morango", (float) 15.30)},
			   b3 = {new SimpleEntry<String,Float>("pessego", (float) 13.00), new SimpleEntry<String,Float>("pera", (float) 12.50)};
		
		//create new buyer agents
		AgentController buyer = this.mainContainer.createNewAgent("buyerAgent", "main.BuyerAgent", b1),
						buyer2 = this.mainContainer.createNewAgent("buyerAgent2", "main.BuyerAgent", b2),
						buyer3 = this.mainContainer.createNewAgent("buyerAgent3", "main.BuyerAgent", b3);
		
		//start new buyer agents 
		buyer.start();
		buyer2.start();
		buyer3.start();
		
		Bid[] s1 = {new Bid("banana", (float)9.00, 10,(float)0.05)},
			s2 = {new Bid("laranja",(float)12.00, 5,(float)0.10)};
		
		//create new seller agents
		AgentController seller = this.mainContainer.createNewAgent("sellerAgent", "main.SellerAgent", s1),
						seller2 = this.mainContainer.createNewAgent("sellerAgent2", "main.SellerAgent", s2);
		
		//start new sellers
		seller.start();
		seller2.start();

	}


}
