package utilities;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import jade.util.Logger;
import main.Bid;

public class SellerStatistics {
	
	private String sellerName;
	private Integer purchaseCancels = 0;
	private ConcurrentHashMap<String, Integer> itemProposes = new ConcurrentHashMap<String, Integer>(); //itemID, Proposes received
	private ArrayList<Bid> itemSells = new ArrayList<Bid>(); //bids made contain initial value for item and sold value
	
	
	public SellerStatistics(String sellerName) {
		this.sellerName = sellerName;
	}
	
	public void incPurchaseCancels() {
		purchaseCancels++;
	}
	
	public void incItemPropose(String itemID) {
		if(itemProposes.containsKey(itemID)) {
			itemProposes.put(itemID, itemProposes.get(itemID) + 1);
		}
		else {
			itemProposes.put(itemID, 1);
		}
	}
	
	public void addItemSell(Bid sell) {
		itemSells.add(sell);
	}
	
	public void logPurchaseCancels(Logger logger) {
		logger.fine("Sales CANCEL received: " + purchaseCancels.toString());
	}
	
	public void logMostBidOn(Logger logger) {
		if(itemProposes.isEmpty()) return;
		
		itemProposes = Utils.sortHashMapByValues(itemProposes);
		
		logger.fine("--Number of times each item was bid-on (PROPOSES)--");
		for(Entry<String, Integer> entry: itemProposes.entrySet()) {
			logger.fine("Item " + entry.getKey() + " was bid-on " + entry.getValue() + " times");
		}
	}
	
	public void logProfit(Logger logger) {
		float totalProfit = 0;
		
		logger.fine("--Profit for each item sell--");
		for(Bid sell: itemSells) {
			float initVal = sell.getInitialValue(),
					soldVal = sell.getValue(),
					itemProfit = soldVal - initVal;
			
			totalProfit += itemProfit;
			
			logger.fine("Item " + sell.getItem() + " was worth " + initVal + " and was sold at " 
					+ soldVal + " totalizing a profit of " + itemProfit);
		}
		
		logger.fine("Total profit: " + totalProfit);
		logger.fine("Average profit per item: " + (float)totalProfit/(float)itemSells.size());
	}
	
	
	public void logStatistics(Logger logger) {
		logger.fine("------------------" + sellerName + " Sells Report------------------");
		logPurchaseCancels(logger);
		logMostBidOn(logger);
		logProfit(logger);
		logger.fine("------------------End of Report------------------");
	}
}
