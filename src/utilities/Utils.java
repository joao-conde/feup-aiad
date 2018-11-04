package utilities;

import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class Utils {
	
	public static final String 
		PURCHASE = "PURCHASE",
		CANCEL = "CANCEL",
		SD_BUY = "SD_BUY",
		SD_SELL = "SD_SELL",	
		CFP_PROTOCOL = "FCFP",
		
		LOG_PATH = "logs/";
	
		
	public static float round(float number, int scale) {
	    int pow = 10;
	    for (int i = 1; i < scale; i++)
	        pow *= 10;
	    float tmp = number * pow;
	    return ( (float) ( (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) ) ) / pow;
	}
	
	public static SimpleFormatter messageFormatter() {
		return new SimpleFormatter() {
	        private static final String format = "[%1$tF %1$tT] %3$s %n";
	
	        @Override
	        public synchronized String format(LogRecord lr) {
	            return String.format(format,
	                    new Date(lr.getMillis()),
	                    lr.getLevel().getLocalizedName(),
	                    lr.getMessage()
	            );
	        }
	    };
	}
		
}
	