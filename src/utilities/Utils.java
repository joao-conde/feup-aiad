package utilities;

public class Utils {

	public static final String DEFAULT_BUYERS_PATH = "data/buyers.xml", DEFAULT_SELLERS_PATH = "data/sellers.xml";

	public static final String SD_BUY = "SD_BUY", SD_SELL = "SD_SELL";

	public static final String PURCHASE = "PURCHASE", WAIT = "WAIT", CANCEL = "CANCEL";

	public static final String CFP_PROTOCOL = "FCFP", RATE = "RATE", NULL = "NULL";

	public static float round(float number, int scale) {
		int pow = 10;
		for (int i = 1; i < scale; i++)
			pow *= 10;
		float tmp = number * pow;
		return ((float) ((int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp))) / pow;
	}

}
