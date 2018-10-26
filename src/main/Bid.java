package main;

public class Bid implements java.io.Serializable{
	

	private static final long serialVersionUID = 1L;
	private float value;
	private String itemID;
	private String lastBidder;
	
	public Bid(String item, float value) {
		this.itemID = item;
		this.value = value;
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
	

	public boolean isGreaterThan(Bid bid) {
		return this.value > bid.getValue();
	}
	

}
