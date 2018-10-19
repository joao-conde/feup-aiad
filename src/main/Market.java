package main;

import java.util.List;
import java.util.Map;

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
		
		
		AgentController buyer = this.mainContainer.acceptNewAgent("buyerAgent", new BuyerAgent());
		buyer.start();
		
		AgentController buyer2 = this.mainContainer.acceptNewAgent("buyerAgent2", new BuyerAgent());
		buyer2.start();
		
		
		AgentController seller = this.mainContainer.acceptNewAgent("sellerAgent", new SellerAgent());
		seller.start();
		
		AgentController seller2 = this.mainContainer.acceptNewAgent("sellerAgent2", new SellerAgent());
		seller2.start();
		/* Initializing another agent with arguments
		Object[] agentArgs = new Object[...];
		AgentController ac2 =
		container.createNewAgent("name2", "jade.core.Agent", agentArgs);
		ac2.start();
		*/
	}

}
