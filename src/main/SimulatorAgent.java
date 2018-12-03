package main;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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
		protected ArrayList<SimpleEntry<Float, Float>> items = new ArrayList<SimpleEntry<Float, Float>>();

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
			items.add(new SimpleEntry(5.0, 1.0));
			items.add(new SimpleEntry(5.0, 2.0));
			items.add(new SimpleEntry(5.0, 3.0));
			items.add(new SimpleEntry(5.0, 6.0));
			items.add(new SimpleEntry(2.0, 1.0));
			items.add(new SimpleEntry(2.0, 2.0));
			items.add(new SimpleEntry(2.0, 3.0));
			items.add(new SimpleEntry(2.0, 4.0));
			items.add(new SimpleEntry(1.0, 1.0));
			items.add(new SimpleEntry(1.0, 2.0));
			items.add(new SimpleEntry(8.0, 2.0));
			items.add(new SimpleEntry(8.0, 3.0));
			items.add(new SimpleEntry(8.0, 4.0));
			items.add(new SimpleEntry(8.0, 6.0));
			items.add(new SimpleEntry(10.0, 1.0));
			items.add(new SimpleEntry(10.0, 2.0));
			items.add(new SimpleEntry(10.0, 4.0));
			items.add(new SimpleEntry(10.0, 6.0));
			items.add(new SimpleEntry(10.0, 8.0));
			items.add(new SimpleEntry(15.0, 1.0));
			items.add(new SimpleEntry(15.0, 2.0));
			items.add(new SimpleEntry(15.0, 3.0));
			items.add(new SimpleEntry(20.0, 2.0));
			items.add(new SimpleEntry(20.0, 6.0));
			items.add(new SimpleEntry(25.0, 1.0));
			items.add(new SimpleEntry(25.0, 8.0));
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
				// System.out.println("Running simulation untill all agents die");

				if (agents == null || agents.size() == 0)
					return;

				for (AgentController agent : agents) {
					try {
						// System.out.println(agent.getState());~

						if (agent.getState().toString().equals("Deleted")) {
							agents.remove(agent);
						}

						else
							return;
					} catch (StaleProxyException e) {
						agents.remove(agent);
						return;
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

			public Boolean generateAgents() throws StaleProxyException {

				AgentController[] buyers = generateBuyers();
				AgentController[] sellers = generateSellers();

				if (buyers == null || sellers == null) {
					System.out.println("There was a problem initializing agents, exiting...");
					return false;
				}

				for (int i = 0; i < buyers.length; i++) {
					buyers[i].start();
					this.agents.add(buyers[i]);
				}

				for (int i = 0; i < sellers.length; i++) {
					sellers[i].start();
					this.agents.add(sellers[i]);
				}

				return true;

			}

			public AgentController[] generateBuyers() throws StaleProxyException {
				Random rand = new Random(System.currentTimeMillis());
				int numberBuyers = rand.nextInt(4) + 1;
				AgentController[] buyerAgents = new AgentController[numberBuyers];
				for (int i = 0; i < numberBuyers; i++) {
					String name = "Buyer " + i;
					int numberofItems = rand.nextInt(26) + 1;
					Object[] itemsEntry = new Object[numberofItems];
					for (int a = 0; a < numberofItems; a++) {
						int itemId = rand.nextInt(items.size());
						String itemName = "item " + itemId;
						Float gaussianValue = new Float(rand.nextGaussian() + "f");
						Float itemPrice = gaussianValue * new Float(items.get(itemId).getValue() + "f")
								+ new Float(items.get(itemId).getKey() + "f");

						itemsEntry[a] = new SimpleEntry<String, Float>(itemName, itemPrice);

					}
					buyerAgents[i] = this.container.createNewAgent(name, "main.BuyerAgent", itemsEntry);

				}
				return buyerAgents;
			}

			public AgentController[] generateSellers() throws StaleProxyException {
				Random rand = new Random(System.currentTimeMillis());
				int numberOfSellers = rand.nextInt(4) + 1;
				AgentController[] sellerAgents = new AgentController[numberOfSellers];
				for (int i = 0; i < numberOfSellers; i++) {
					String name = "Seller " + i;
					int numberOfItems = rand.nextInt(26) + 1;
					Object[] bidsEntry = new Object[numberOfItems + 1];
					bidsEntry[0] = rand.nextInt(3);
					for (int a = 1; a < numberOfItems + 1; a++) {
						int itemId = rand.nextInt(items.size());
						String itemName = "item " + itemId;
						Float gaussianValue = new Float(rand.nextGaussian() + "f");
						Float itemPrice = gaussianValue * new Float(items.get(itemId).getValue() + "f")
								+ new Float(items.get(itemId).getKey() + "f");
						Float inc = 0.25f + rand.nextFloat() * (2.0f - 0.25f);
						Integer delivery = rand.nextInt(5) + 1;
						bidsEntry[a] = new Bid(itemName, itemPrice, delivery, inc);
					}

					sellerAgents[i] = this.container.createNewAgent(name, "main.SellerAgent", bidsEntry);
				}
				return sellerAgents;
			}
		}

	}

}
