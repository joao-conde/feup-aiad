package main;

public class Bid extends Auction{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private float minIncrease; //minimum value that a buyer has to increase to the last value
	private String lastBidder; //propose agent name
	
	public Bid(String item, float value, int deliveryTime, float minIncrease) {
		super(item, value, deliveryTime);
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
	
}
