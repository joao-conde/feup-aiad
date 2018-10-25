package main;

public class Item implements java.io.Serializable{
	

	private static final long serialVersionUID = 1L;
	private String name;
	private float price;
	
	public Item(String name, float price) {
		this.name = name;
		this.price = price;
	}
	
	public Item(String name) {
		this.name = name;
	}

	public void setPrice(float price) {
		this.price = price;
	}
	
	public String getName() {
		return this.name;
	}
	
	public float getPrice() {
		return this.price;
	}
	
	@Override
	public boolean equals(Object obj) {
		Item item = (Item) obj;
		if(item.getName().equals(this.getName()))
			return true;
		else
			return false;
	}
	
}
