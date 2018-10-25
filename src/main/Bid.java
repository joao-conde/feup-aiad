package main;

public class Bid {
	
	private Integer value;
	private Item proposedItem;
	
	public Bid(Item item, Integer value) {
		this.value = value;
		this.proposedItem = item;
	}
	
	public Integer getValue() {
		return this.value;
	}
	
	public void setItem(Item item) {
		this.proposedItem = item;
	}
	

}
