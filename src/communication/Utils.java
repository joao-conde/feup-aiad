package communication;

public class Utils {
	
	public static final String 
		PURCHASE = "PURCHASE",
		CANCEL = "CANCEL",
		SD_BUY = "SD_BUY",
		SD_SELL = "SD_SELL",
		
		CFP_PROTOCOL = "FCFP";
		
	public static float round(float number, int scale) {
	    int pow = 10;
	    for (int i = 1; i < scale; i++)
	        pow *= 10;
	    float tmp = number * pow;
	    return ( (float) ( (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) ) ) / pow;
	}						
		
}
	