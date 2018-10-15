package main;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;

public class Market {

	public static void main(String[] args) {
		
		//Get a JADE runtime
		Runtime rt = Runtime.instance(); 
		
		//Create the main container
		Profile p1 = new ProfileImpl();
		//p1.setParameter(...);
		// optional
		ContainerController mainContainer = rt.createMainContainer(p1);
		
		//Create an additional container
		Profile p2 = new ProfileImpl();
		//p2.setParameter(...);
		// optional
		ContainerController container = rt.createAgentContainer(p2);
		
		//Launch agent
		/*AgentController ac1 = container.acceptNewAgent("name1", new Agent());
		ac1.start();
		Object[] agentArgs = new Object[...];
		AgentController ac2 =
		container.createNewAgent("name2", "jade.core.Agent", agentArgs);
		ac2.start();*/

	}

}
