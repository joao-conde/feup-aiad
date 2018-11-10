package utilities;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import jade.util.Logger;
import main.Purchase;

public class BuyerStatistics {
	
	private String buyerName;
	private Integer desiredItems;
	private ConcurrentHashMap<String, ArrayList<String>> itemAttempts = new ConcurrentHashMap<String, ArrayList<String>>(); //itemID and auctions(sellers) participated
	private ConcurrentHashMap<String, Integer> itemAuctionsWon = new ConcurrentHashMap<String, Integer>(); //itemID and auctions won
	private ArrayList<Purchase> purchases = new ArrayList<Purchase>();
	
	public BuyerStatistics(String buyerName, int desiredItems) {
		this.buyerName = buyerName;
		this.desiredItems = desiredItems;
	}
	
	public void incItemAttempts(String itemID, String sellerID) {
		if(itemAttempts.containsKey(itemID)) {
			if(!itemAttempts.get(itemID).contains(sellerID)) itemAttempts.get(itemID).add(sellerID);
		}
		else {
			itemAttempts.put(itemID, new ArrayList<String>());
		}
	}
	
	public void incItemAuctionsWon(String itemID) {
		if(itemAuctionsWon.containsKey(itemID)) {
			itemAuctionsWon.put(itemID, itemAuctionsWon.get(itemID)+1);
		}
		else {
			itemAuctionsWon.put(itemID, 1);
		}
	}
	
	public void addPurchase(Purchase p) {
		purchases.add(p);
	}
	
	public void logPercentageBought(Logger logger) {
		float perBought = 100 * (float)(float)purchases.size()/desiredItems;
		logger.fine(buyerName + " bought " + perBought + "% of what he desired");
	}
	
	public void logAttempts(Logger logger) {
		if(itemAttempts.size() <= 0) return;
		
		float attempts = 0, winRate;
		for(Entry<String, ArrayList<String>> entry: itemAttempts.entrySet()) {
			attempts += entry.getValue().size();
			logger.fine(buyerName + " attempted " + entry.getValue().size() + " auctions to buy " + entry.getKey());
		}
		winRate = 100 * (float)purchases.size()/(float)attempts;
		logger.fine(buyerName + " wins on average " + winRate + "% of the auctions he participates in");
	}
	
	public void logExtraSpent(Logger logger) {
		if(purchases.size() <= 0) return;
		
		float extraSpent = 0;
		logger.fine("--In order to win auctions " + buyerName + " spent--");
		for(Purchase p: purchases) {
			extraSpent += p.getExtraPaid();
			logger.fine("For item " + p.getItemID() + ", " + buyerName + " paid an extra " 
					+ p.getExtraPaid() + " relatively to the item's base value");
		}
		logger.fine("On average, " + buyerName + " spent " 
				+ extraSpent/(float)purchases.size() + " more per item than it's base value");
	}
	
	public void logStatistics(Logger logger) {
		logger.fine("------------------" + buyerName + " Purchases Report------------------");
		logPercentageBought(logger);
		logAttempts(logger);
		logExtraSpent(logger);
		
		logger.fine("------------------End of Report------------------");
	}
}
