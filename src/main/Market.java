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
import jade.tools.sniffer.ExitAction;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import utilities.Utils;

public class Market { 
		
	private Runtime jadeRt; //JADE runtime
	private ContainerController mainContainer; //Main JADE container
	 
	public static void main(String[] args) {
		if(args.length == 2) {
			new Market(args[0],args[1]);
		}else {
			new Market(Utils.DEFAULT_BUYERS_PATH,Utils.DEFAULT_SELLERS_PATH);
		}
		
	}
	
	public Market(String buyersFile,String sellersFile) {
		try {
			new File(Utils.LOG_PATH).mkdirs();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*Object[] b1 = {new SimpleEntry<String,Float>("batatas",(float)15.0)},
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
		seller2.start();*/

	}
	
	public AgentController[] initializeBuyers(String filepath) throws ParserConfigurationException, SAXException, IOException, StaleProxyException {
		
		File xml = new File(filepath);
		if(!xml.exists() || xml.isDirectory())
			return null;
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xml);
		
		
		doc.getDocumentElement().normalize();
		
		System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
		
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
		System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
		
		NodeList sellers = doc.getElementsByTagName("seller");
		AgentController[] sellerAgents = new AgentController[sellers.getLength()];
		for(int i = 0; i < sellers.getLength();i++) {
			Node nseller = sellers.item(i);
			
			if(nseller.getNodeType() == Node.ELEMENT_NODE) {
				Element seller = (Element) nseller;
				String name = seller.getAttribute("name");
				Integer credibility = Integer.parseInt(seller.getAttribute("credibility")); 
				NodeList bids = seller.getElementsByTagName("bid");
				Object[] bidsEntry = new Object[bids.getLength()+1];
				bidsEntry[0] = credibility;
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
