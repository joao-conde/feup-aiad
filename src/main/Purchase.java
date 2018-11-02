package main;

public class Purchase extends Auction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int realDeliveryTime;
	
	
	public Purchase(String item, float value, int deliveryTime, int realDeliveryTime) {
		super(item, value, deliveryTime);
		this.realDeliveryTime = realDeliveryTime;
	}

}
