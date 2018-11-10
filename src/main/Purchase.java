package main;

import main.Bid;

public class Purchase implements java.io.Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String itemID;
	private String sellerID;
	private float rating;
	private float extraPaid; //difference between base price and actually paid for the item
	
	public Purchase(Bid bid, String sellerID, int realDeliveryTime, float maxValue) {
		this.itemID = bid.getItem();
		this.sellerID = sellerID;	
		this.extraPaid = maxValue - bid.getValue();
		computeRating(realDeliveryTime, bid.getDeliveryTime(), bid.getValue(), maxValue, bid.getInitialValue());
	}
	
	private void computeRating(int realDeliveryTime, int expectedDeliveryTime, float value, float maxValue, float initVal) {
		float timeRating = ((float)expectedDeliveryTime/((float)expectedDeliveryTime + (float)realDeliveryTime)),
			  valueRating = (maxValue - value)/(maxValue - initVal);
		this.rating = (float) (timeRating * 0.5 + valueRating * 0.5);
	}
	
	public float getExtraPaid() {
		return this.extraPaid;
	}
	
	public String getItemID() {
		return itemID;
	}


	public void setItemID(String itemID) {
		this.itemID = itemID;
	}


	public String getSellerID() {
		return sellerID;
	}


	public void setSellerID(String sellerID) {
		this.sellerID = sellerID;
	}


	public float getRating() {
		return rating;
	}


	public void setRating(float rating) {
		this.rating = rating;
	}

}
