package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SSResponderDispatcher;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import utilities.Utils;

public class SimulatorAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Integer messagesToReceive, messagesReceived = 0;
	private HashMap<String, Integer> itemsSold = new HashMap<String, Integer>();

	protected void setup() {
		Object[] args = this.getArguments();

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());

		ServiceDescription sd = new ServiceDescription();
		sd.setType(Utils.SIMULATOR_AGENT);
		sd.setName("Simulator");
		dfd.addServices(sd);

		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new LaunchSimulationsBehaviour(this, Integer.parseInt(args[0].toString())));
		addBehaviour(new InformDispatcher(this, MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
	}

	private class InformDispatcher extends SSResponderDispatcher {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public InformDispatcher(Agent a, MessageTemplate tpl) {
			super(a, tpl);
		}

		@Override
		protected Behaviour createResponder(ACLMessage inform) {
			return new InformHandler(myAgent, inform);
		}

		private class InformHandler extends Behaviour {

			private static final long serialVersionUID = 1L;

			ACLMessage msg;
			boolean finished = false;

			public InformHandler(Agent myAgent, ACLMessage inform) {
				this.msg = inform;
				this.myAgent = myAgent;
			}

			@Override
			public void action() {
				messagesReceived++;
				try {
					if (msg.getContentObject() != null) {
						ArrayList<Bid> itemsSold = ((ArrayList<Bid>) msg.getContentObject());

						/*
						 * for(Bid item: itemsSold) { TODO: associate for each of the table entrys if
						 * product was sold or not }
						 */
					}
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				finished = true;
			}

			@Override
			public boolean done() {
				return finished;
			}

		}
	}

	private class LaunchSimulationsBehaviour extends SimpleBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		protected String csvPath = "simulations/";
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
			new File(csvPath).mkdirs();

			// ensure log folder exist if logs uncommented in agents
			/*
			 * MarketLogger.calculateLogPath(); new File(MarketLogger.logPath).mkdirs();
			 */
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

			this.currentSimulation = new Simulation(simulations);
			System.out.println("Running simulation nº" + simulations + "...");
			addBehaviour(currentSimulation);
			simulations--;
		}

		@Override
		public boolean done() {
			return finished;
		}

		@Override
		public int onEnd() {
			System.exit(0);
			return simulations;
		}

		private void createItems() {
			items.add(new SimpleEntry(5.0, 1.0));
			/*
			 * items.add(new SimpleEntry(5.0, 2.0)); items.add(new SimpleEntry(5.0, 3.0));
			 */
			/*
			 * items.add(new SimpleEntry(5.0, 6.0)); items.add(new SimpleEntry(2.0, 1.0));
			 * items.add(new SimpleEntry(2.0, 2.0)); items.add(new SimpleEntry(2.0, 3.0));
			 * items.add(new SimpleEntry(2.0, 4.0)); items.add(new SimpleEntry(1.0, 1.0));
			 * items.add(new SimpleEntry(1.0, 2.0)); items.add(new SimpleEntry(8.0, 2.0));
			 * items.add(new SimpleEntry(8.0, 3.0)); items.add(new SimpleEntry(8.0, 4.0));
			 * items.add(new SimpleEntry(8.0, 6.0)); items.add(new SimpleEntry(10.0, 1.0));
			 * items.add(new SimpleEntry(10.0, 2.0)); items.add(new SimpleEntry(10.0, 4.0));
			 * items.add(new SimpleEntry(10.0, 6.0)); items.add(new SimpleEntry(10.0, 8.0));
			 * items.add(new SimpleEntry(15.0, 1.0)); items.add(new SimpleEntry(15.0, 2.0));
			 * items.add(new SimpleEntry(15.0, 3.0)); items.add(new SimpleEntry(20.0, 2.0));
			 * items.add(new SimpleEntry(20.0, 6.0)); items.add(new SimpleEntry(25.0, 1.0));
			 * items.add(new SimpleEntry(25.0, 8.0));
			 */
		}

		private class Simulation extends SimpleBehaviour {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			private ContainerController container;
			private Integer simulationNumber;

			private ArrayList<String[]> tableEntry = new ArrayList<String[]>(); // item, sellerName, sellerDelay,
																				// median, variance, initialValue
			private HashMap<String, Integer> itemInterest = new HashMap<String, Integer>();
			private HashMap<String, Integer> itemAuctions = new HashMap<String, Integer>();

			public Boolean finished = false;

			public Simulation(Integer simulationNo) {
				messagesToReceive = 0;
				messagesReceived = 0;
				itemsSold = null;
				simulationNumber = simulationNo;
				try {
					this.createContainer();
					this.generateAgents();
				} catch (SecurityException | StaleProxyException | ParserConfigurationException | SAXException
						| IOException e) {
					e.printStackTrace();
				}

			}

			@Override
			public void action() {
				finished = (messagesToReceive == messagesReceived);
			}

			@Override
			public int onEnd() {
				try {
					FileOutputStream csvOut = new FileOutputStream(new File(csvPath + simulationNumber + ".csv"));

					// item, sellerName, sellerDelay, median, variance, initialValue
					for (String[] entry : tableEntry) {
						System.out.println(entry);
						byte[] strBytes = (entry[0] + ", " + entry[1] + ", " + entry[2] + ", " + entry[3] + ", "
								+ entry[4] + ", " + entry[5] + "\n").getBytes();
						csvOut.write(strBytes);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

				System.out.println("Simulation finished successfully\n");
				return 0;
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

			public Boolean generateAgents()
					throws StaleProxyException, ParserConfigurationException, SAXException, IOException {

				/*
				 * AgentController[] buyers = generateBuyers(); AgentController[] sellers =
				 * generateSellers();
				 */

				AgentController[] buyers = this.initializeBuyers("./data/SimpleExample/buyers.xml");
				AgentController[] sellers = this.initializeSellers("./data/SimpleExample/sellers.xml");

				if (buyers == null || sellers == null) {
					System.out.println("There was a problem initializing agents, exiting...");
					return false;
				}

				for (int i = 0; i < buyers.length; i++) {
					buyers[i].start();
					messagesToReceive++;
				}

				for (int i = 0; i < sellers.length; i++) {
					sellers[i].start();
					messagesToReceive++;
				}

				return true;

			}

			public AgentController[] initializeBuyers(String filepath)
					throws ParserConfigurationException, SAXException, IOException, StaleProxyException {

				File xml = new File(filepath);
				if (!xml.exists() || xml.isDirectory())
					return null;

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(xml);

				doc.getDocumentElement().normalize();

				NodeList buyers = doc.getElementsByTagName("buyer");
				AgentController[] buyerAgents = new AgentController[buyers.getLength()];
				for (int i = 0; i < buyers.getLength(); i++) {
					Node nbuyer = buyers.item(i);

					if (nbuyer.getNodeType() == Node.ELEMENT_NODE) {
						Element buyer = (Element) nbuyer;
						String name = buyer.getAttribute("name");
						NodeList items = buyer.getElementsByTagName("item");
						Object[] itemsEntry = new Object[items.getLength()];
						for (int a = 0; a < items.getLength(); a++) {
							Element item = (Element) items.item(a);
							itemsEntry[a] = new SimpleEntry<String, Float>(item.getTextContent(),
									Float.parseFloat(item.getAttribute("maxvalue")));
						}
						buyerAgents[i] = this.container.createNewAgent(name + this.simulationNumber, "main.BuyerAgent",
								itemsEntry);

					}

				}

				return buyerAgents;
			}

			public AgentController[] initializeSellers(String filepath)
					throws ParserConfigurationException, SAXException, IOException, StaleProxyException {
				File xml = new File(filepath);
				if (!xml.exists() || xml.isDirectory())
					return null;

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(xml);

				doc.getDocumentElement().normalize();

				NodeList sellers = doc.getElementsByTagName("seller");
				AgentController[] sellerAgents = new AgentController[sellers.getLength()];
				for (int i = 0; i < sellers.getLength(); i++) {
					Node nseller = sellers.item(i);

					if (nseller.getNodeType() == Node.ELEMENT_NODE) {
						Element seller = (Element) nseller;
						String name = seller.getAttribute("name");
						Integer shipmentDelay = Integer.parseInt(seller.getAttribute("shipmentDelay"));
						NodeList bids = seller.getElementsByTagName("bid");
						Object[] bidsEntry = new Object[bids.getLength() + 1];
						bidsEntry[0] = shipmentDelay;
						for (int a = 0; a < bids.getLength(); a++) {
							Element nbid = (Element) bids.item(a);
							Element item = (Element) nbid.getElementsByTagName("item").item(0);
							int delivery = Integer
									.parseInt(nbid.getElementsByTagName("delivery").item(0).getTextContent());
							float increase = Float
									.parseFloat(nbid.getElementsByTagName("increase").item(0).getTextContent());
							bidsEntry[a + 1] = new Bid(item.getTextContent(),
									Float.parseFloat(item.getAttribute("price")), delivery, increase);
						}

						sellerAgents[i] = this.container.createNewAgent(name + this.simulationNumber,
								"main.SellerAgent", bidsEntry);
					}
				}

				return sellerAgents;

			}

			public AgentController[] generateBuyers() throws StaleProxyException {
				Random rand = new Random(System.currentTimeMillis());

				// int numberBuyers = rand.nextInt(15) + 2;
				int numberBuyers = 3;
				AgentController[] buyerAgents = new AgentController[numberBuyers];

				for (int i = 0; i < numberBuyers; i++) {
					String name = "Buyer " + i;
					int numberofItems = 1; // rand.nextInt(items.size()) + 1;

					Object[] itemsEntry = new Object[numberofItems];
					ArrayList<String> itemsNames = new ArrayList<String>();
					for (int a = 0; a < numberofItems; a++) {

						int itemId = rand.nextInt(items.size());
						String itemName = "item " + itemId;
						Float gaussianValue = new Float(rand.nextGaussian() + "f");
						Float itemPrice = gaussianValue * new Float(items.get(itemId).getValue() + "f")
								+ new Float(items.get(itemId).getKey() + "f");

						if (itemsNames.contains(itemName)) {
							a--;
							continue;
						}
						itemsEntry[a] = new SimpleEntry<String, Float>(itemName, itemPrice);
						itemsNames.add(itemName);

						if (itemInterest.keySet().contains(itemName))
							itemInterest.put(itemName, itemInterest.get(itemName).intValue() + 1);
						else
							itemInterest.put(itemName, 1);

					}
					buyerAgents[i] = this.container.createNewAgent(name + this.simulationNumber, "main.BuyerAgent", itemsEntry);

				}
				return buyerAgents;
			}

			public AgentController[] generateSellers() throws StaleProxyException {
				Random rand = new Random(System.currentTimeMillis());

				// int numberOfSellers = rand.nextInt(3) + 1;
				int numberOfSellers = 1;
				AgentController[] sellerAgents = new AgentController[numberOfSellers];
				for (int i = 0; i < numberOfSellers; i++) {
					String name = "Seller " + i;
					// int numberOfItems = rand.nextInt(items.size()) + 1;
					int numberOfItems = 1;
					Object[] bidsEntry = new Object[numberOfItems + 1];
					ArrayList<String> itemsName = new ArrayList<String>();
					Integer sellerDelay = rand.nextInt(3);
					bidsEntry[0] = sellerDelay;
					for (int a = 1; a < numberOfItems + 1; a++) {
						int itemId = rand.nextInt(items.size());
						String itemName = "item " + itemId;
						Float gaussianValue = new Float(rand.nextGaussian() + "f");
						Float itemPrice = gaussianValue * new Float(items.get(itemId).getValue() + "f")
								+ new Float(items.get(itemId).getKey() + "f");
						Float inc = 0.25f + rand.nextFloat() * (2.0f - 0.25f);
						Integer delivery = rand.nextInt(5) + 1;

						if (itemsName.contains(itemName)) {
							a--;
							continue;
						}
						bidsEntry[a] = new Bid(itemName, itemPrice, delivery, inc);
						itemsName.add(itemName);

						if (itemAuctions.keySet().contains(itemName))
							itemAuctions.put(itemName, itemAuctions.get(itemName).intValue() + 1);
						else
							itemAuctions.put(itemName, 1);

						// item, sellerName, sellerDelay, median, variance, initialValue
						String[] entry = { itemName, name, sellerDelay.toString(),
								items.get(itemId).getKey() + "f".toString(),
								items.get(itemId).getValue() + "f".toString(), itemPrice.toString() };
						tableEntry.add(entry);
					}

					sellerAgents[i] = this.container.createNewAgent(name + this.simulationNumber, "main.SellerAgent", bidsEntry);
				}
				return sellerAgents;
			}

		}

	}

}
