package main;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.core.behaviours.SimpleBehaviour;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import utilities.MarketLogger;

public class SimulatorAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void setup() {
		Object[] args = this.getArguments();
		addBehaviour(new LaunchSimulationsBehaviour(this, Integer.parseInt(args[0].toString())));
	}

	private class LaunchSimulationsBehaviour extends SimpleBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		protected Boolean finished = false;
		protected Integer simulations;
		protected Simulation currentSimulation = null;
		protected Runtime jadeRt;
		protected HashMap<String, SimpleEntry<Float, Float>> items = new HashMap<String, SimpleEntry<Float, Float>>();

		public LaunchSimulationsBehaviour(Agent myAgent, Integer simulations) {
			this.myAgent = myAgent;
			this.simulations = simulations;
			this.jadeRt = Runtime.instance();
			this.createItems();
		}

		@Override
		public void action() {

			if (currentSimulation != null && !currentSimulation.finished)
				return;

			if (simulations <= 0) {
				System.out.println("No more simulations to run");
				finished = true;
				return;
			}

			this.currentSimulation = new Simulation();
			System.out.println("Launched simulaton number " + simulations);
			addBehaviour(currentSimulation);
			simulations--;
		}

		@Override
		public boolean done() {
			return finished;
		}
		
		private void createItems() {
			items.put("a", new SimpleEntry(5.0, 1.0));
			items.put("b", new SimpleEntry(5.0, 2.0));
			items.put("c", new SimpleEntry(5.0, 3.0));
			items.put("d", new SimpleEntry(5.0, 6.0));
			items.put("e", new SimpleEntry(2.0, 1.0));
			items.put("f", new SimpleEntry(2.0, 2.0));
			items.put("g", new SimpleEntry(2.0, 3.0));
			items.put("h", new SimpleEntry(2.0, 4.0));
			items.put("i", new SimpleEntry(1.0, 1.0));
			items.put("j", new SimpleEntry(1.0, 2.0));
			items.put("k", new SimpleEntry(8.0, 2.0));
			items.put("l", new SimpleEntry(8.0, 3.0));
			items.put("m", new SimpleEntry(8.0, 4.0));
			items.put("n", new SimpleEntry(8.0, 6.0));
			items.put("o", new SimpleEntry(10.0, 1.0));
			items.put("p", new SimpleEntry(10.0, 2.0));
			items.put("q", new SimpleEntry(10.0, 4.0));
			items.put("r", new SimpleEntry(10.0, 6.0));
			items.put("s", new SimpleEntry(10.0, 8.0));
			items.put("t", new SimpleEntry(15.0, 1.0));
			items.put("u", new SimpleEntry(15.0, 2.0));
			items.put("v", new SimpleEntry(15.0, 3.0));
			items.put("w", new SimpleEntry(20.0, 2.0));
			items.put("x", new SimpleEntry(20.0, 6.0));
			items.put("y", new SimpleEntry(25.0, 1.0));
			items.put("z", new SimpleEntry(25.0, 8.0));
		}

		private class Simulation extends SimpleBehaviour {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			private ArrayList<AgentController> agents = new ArrayList<AgentController>();
			private ContainerController container;

			public Boolean finished = false;

			public Simulation() {
				try {
					MarketLogger.calculateLogPath();
					new File(MarketLogger.logPath).mkdirs();
					this.createContainer();
					this.generateAgents();
				} catch (SecurityException | StaleProxyException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void action() {
				System.out.println("Running simulation untill all agents die");
				
				for(AgentController agent: agents) {
					try {
						System.out.println(agent.getState());
						if(agent.getState().toString().equals("Deleted")) {
							agents.remove(agent);
						}
						else return;
					} catch (StaleProxyException e) {
						e.printStackTrace();
					}
				}
				
				finished = true;
				System.out.println("Simulation over");
			}

			@Override
			public boolean done() {
				return finished;
			}
			
			public void createContainer() {
				Profile profile = new ProfileImpl();
				profile.setParameter(Profile.CONTAINER_NAME, "Simulation");
				profile.setParameter(Profile.MAIN_HOST, "localhost");
				this.container = jadeRt.createAgentContainer(profile);
			}

			public void generateAgents() throws StaleProxyException {

				AgentController[] buyers = generateBuyers();
				AgentController[] sellers = generateSellers();

				if (buyers == null || sellers == null) {
					System.out.println("There was a problem initializing agents, exiting...");
					return;
				}

				for (int i = 0; i < buyers.length; i++) {
					buyers[i].start();
				}

				for (int i = 0; i < sellers.length; i++) {
					sellers[i].start();
				}

			}

			public AgentController[] generateBuyers() {
				return null;
			}

			public AgentController[] generateSellers() {
				return null;
			}
		}

	}

}
