package main;

public class Bid implements java.io.Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private float minIncrease; //minimum value that a buyer has to increase to the last value
	private String lastBidder; //propose agent name
	private String itemID; //item name
	private float value, initialValue; //euros
	private int deliveryTime; //days
	
	public Bid(String item, float value, int deliveryTime, float minIncrease) {
		this.itemID = item;
		this.value = value;
		this.initialValue = value;
		this.deliveryTime = deliveryTime;
		this.minIncrease = minIncrease;
		this.lastBidder = null;
	}
	
	public void setLastBidder(String lastBidder) {
		this.lastBidder = lastBidder;
	}
	
	
	public String getLastBidder() {
		return this.lastBidder;
	}
	
	
	
	public float getMinIncrease() {
		return this.minIncrease;
	}
	
	public void setNewValue(float newValue) {
		this.value = newValue;
	}
	
	public float getValue() {
		return this.value;
	}
	
	public float getInitialValue() {
		return this.initialValue;
	}
	
	public String getItem() {
		return this.itemID;
	}
	
	public int getDeliveryTime() {
		return this.deliveryTime;
	}
	
}
