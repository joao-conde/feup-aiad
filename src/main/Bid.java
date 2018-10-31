package main;

public class Bid implements java.io.Serializable{
	
	
	private static final long serialVersionUID = -8354813310432147997L;
	private float value=111111; //euros
	private float minIncrease; //minimum value that a buyer has to increase to the last value
	private String itemID; //item name
	private String lastBidder; //propose agent name
	private int deliveryTime; //days
	
	public Bid(String item, float value, int deliveryTime, float minIncrease) {
		this.itemID = item;
		this.value = value;
		this.deliveryTime = deliveryTime;
		this.minIncrease = minIncrease;
		this.lastBidder = null;
	}
	
	public void setLastBidder(String lastBidder) {
		this.lastBidder = lastBidder;
	}
	
	public void setNewValue(float newValue) {
		this.value = newValue;
	}
	
	public float getValue() {
		return this.value;
	}
	
	public String getItem() {
		return this.itemID;
	}
	
	public String getLastBidder() {
		return this.lastBidder;
	}
	
	public int getDeliveryTime() {
		return this.deliveryTime;
	}
	
	public float getMinIncrease() {
		return this.minIncrease;
	}
	
	@Override
	public boolean equals(Object obj) {
		Bid bid = (Bid) obj;
		if(this.value == bid.value && this.itemID == bid.itemID && this.lastBidder == bid.lastBidder)
			return true;
		return false;
	}
	

}
