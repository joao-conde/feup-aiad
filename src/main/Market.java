package main;

import java.io.File;
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

import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import utilities.MarketLogger;
import utilities.Utils;

public class Market { 
		
	private Runtime jadeRt; //JADE runtime
	private ContainerController mainContainer; //Main JADE container
	 
	public static void main(String[] args) {
		if(args.length == 2) {
			new Market(args[0], args[1]);
		}
		else if(args.length == 0){
			new Market(Utils.DEFAULT_BUYERS_PATH, Utils.DEFAULT_SELLERS_PATH);
		}
		else
			System.out.println("INVALID PROGRAM ARGUMENTS");		
	}
	
	public Market(String buyersFile, String sellersFile) {
		try {
			MarketLogger.calculateLogPath();
			new File(MarketLogger.logPath).mkdirs();
			this.initializeContainers();
			this.initializeAgents(buyersFile,sellersFile);
		} catch (SecurityException | StaleProxyException e) {
			e.printStackTrace();
		}	
				
	}
		
	public void initializeContainers() {
		this.jadeRt = Runtime.instance();
		this.mainContainer = jadeRt.createMainContainer(new ProfileImpl());
	}
	
	public void initializeAgents(String buyersFile,String sellersFile) throws StaleProxyException {

		
		try {
			AgentController[] buyers = initializeBuyers(buyersFile);
			AgentController[] sellers = initializeSellers(sellersFile);
			
			if(buyers == null || sellers == null) {
				System.out.println("There was a problem initializing agents, exiting...");
				System.exit(1);
			}
			
			for(int i = 0; i < buyers.length; i++) {
				buyers[i].start();
			}
			
			for(int i = 0; i < sellers.length; i++) {
				sellers[i].start();
			}
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public AgentController[] initializeBuyers(String filepath) throws ParserConfigurationException, SAXException, IOException, StaleProxyException {
		
		File xml = new File(filepath);
		if(!xml.exists() || xml.isDirectory())
			return null;
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xml);
		
		
		doc.getDocumentElement().normalize();
				
		NodeList buyers = doc.getElementsByTagName("buyer");
		AgentController[] buyerAgents = new AgentController[buyers.getLength()];
		for(int i = 0; i < buyers.getLength(); i++) {
			Node nbuyer = buyers.item(i);
			
			if(nbuyer.getNodeType() == Node.ELEMENT_NODE) {
				Element buyer = (Element) nbuyer;
				String name = buyer.getAttribute("name");
				NodeList items = buyer.getElementsByTagName("item");
				Object[] itemsEntry=  new Object[items.getLength()];
				for(int a = 0; a < items.getLength(); a++) {
					Element item = (Element) items.item(a);
					itemsEntry[a] = new SimpleEntry<String,Float>(item.getTextContent(),Float.parseFloat(item.getAttribute("maxvalue")));
				}
				buyerAgents[i] = this.mainContainer.createNewAgent(name,"main.BuyerAgent", itemsEntry);
				
			}
			
		}
		
		return buyerAgents;
	}
	
	public AgentController[] initializeSellers(String filepath) throws ParserConfigurationException, SAXException, IOException, StaleProxyException {
		File xml = new File(filepath);
		if(!xml.exists() || xml.isDirectory())
			return null;
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xml);
		
		doc.getDocumentElement().normalize();
		
		NodeList sellers = doc.getElementsByTagName("seller");
		AgentController[] sellerAgents = new AgentController[sellers.getLength()];
		for(int i = 0; i < sellers.getLength();i++) {
			Node nseller = sellers.item(i);
			
			if(nseller.getNodeType() == Node.ELEMENT_NODE) {
				Element seller = (Element) nseller;
				String name = seller.getAttribute("name");
				Integer shipmentDelay = Integer.parseInt(seller.getAttribute("shipmentDelay")); 
				NodeList bids = seller.getElementsByTagName("bid");
				Object[] bidsEntry = new Object[bids.getLength()+1];
				bidsEntry[0] = shipmentDelay;
				for(int a=0; a < bids.getLength(); a++) {
					Element nbid = (Element) bids.item(a);
					Element item = (Element) nbid.getElementsByTagName("item").item(0);
					int delivery = Integer.parseInt(nbid.getElementsByTagName("delivery").item(0).getTextContent());
					float increase = Float.parseFloat(nbid.getElementsByTagName("increase").item(0).getTextContent());
					bidsEntry[a+1] = new Bid(item.getTextContent(),Float.parseFloat(item.getAttribute("price")),delivery,increase);
				}
				
				sellerAgents[i] = this.mainContainer.createNewAgent(name,"main.SellerAgent",bidsEntry);
			}
		}
		
		return sellerAgents;
		
	}


}
