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
		
		/*AgentController buyer = this.mainContainer.acceptNewAgent("buyerAgent", new BuyerAgent());
		buyer.start();
		*/
		
		Object[] b1 = {"banana"},
				 b2 = {"laranja"};
		
		AgentController buyer = this.mainContainer.createNewAgent("buyerAgent", "main.BuyerAgent", b1),
						buyer2 = this.mainContainer.createNewAgent("buyerAgent2", "main.BuyerAgent", b2);
		
		buyer.start();
		buyer2.start();
		
		
		Object[] s1 = {"banana"},
				 s2 = {"laranja"};
		
		AgentController seller = this.mainContainer.createNewAgent("sellerAgent", "main.SellerAgent", s1),
						 seller2 = this.mainContainer.createNewAgent("sellerAgent2", "main.SellerAgent", s2);
		
		seller.start();
		seller2.start();

	}

}
