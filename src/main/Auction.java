package main;

public class Auction implements java.io.Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String itemID; //item name
	private float value; //euros
	private int deliveryTime; //days

	public Auction(String item, float value, int deliveryTime) {
		this.itemID = item;
		this.value = value;
		this.deliveryTime = deliveryTime;
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
	
	public int getDeliveryTime() {
		return this.deliveryTime;
	}
	
	/*@Override
	public boolean equals(Object obj) {
		Bid bid = (Bid) obj;
		return (this.itemID == bid.itemID);
	}*/

}
