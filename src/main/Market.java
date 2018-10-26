package main;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;

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
		/*	Creating a new container, must pass different ProfileImpl and cannot be a main container
		ContainerController container = jadeRt.createAgentContainer(new ProfileImpl());
		*/
	}
	
	public void initializeAgents() throws StaleProxyException {
		
		/*AgentController buyer = this.mainContainer.acceptNewAgent("buyerAgent", new BuyerAgent());
		buyer.start();
		*/
		
		String[] b1 = {"banana", "pessego"},
			   b2 = {"banana", "morango"},
			   b3 = {"pessego", "pera"};
		
		AgentController buyer = this.mainContainer.createNewAgent("buyerAgent", "main.BuyerAgent", b1),
						buyer2 = this.mainContainer.createNewAgent("buyerAgent2", "main.BuyerAgent", b2),
						buyer3 = this.mainContainer.createNewAgent("buyerAgent3", "main.BuyerAgent", b3);
		
		buyer.start();
		buyer2.start();
		buyer3.start();
		
		Bid[] s1 = {new Bid("banana", (float)10.0)},
				 s2 = {new Bid("laranja",(float)12)};
		
		AgentController seller = this.mainContainer.createNewAgent("sellerAgent", "main.SellerAgent", s1),
						seller2 = this.mainContainer.createNewAgent("sellerAgent2", "main.SellerAgent", s2);
		
		seller.start();
		seller2.start();

	}

}
