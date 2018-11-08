package utilities;

import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import jade.util.Logger;
import main.BuyerAgent;

public class Utils {
	
	
	public static final String
			DEFAULT_BUYERS_PATH = "data/buyers.xml",
			DEFAULT_SELLERS_PATH= "data/sellers.xml";
	
	public static final String 
			SD_BUY = "SD_BUY",
			SD_SELL = "SD_SELL";
	
	public static final String 
		PURCHASE = "PURCHASE",
		WAIT = "WAIT",
		CANCEL = "CANCEL";
	
	public static final String
		CFP_PROTOCOL = "FCFP",
		RATE = "RATE";
		
	public static final String
		LOG_PATH = "logs/";
		
	public static float round(float number, int scale) {
	    int pow = 10;
	    for (int i = 1; i < scale; i++)
	        pow *= 10;
	    float tmp = number * pow;
	    return ( (float) ( (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) ) ) / pow;
	}
	
	public static Logger createLogger(String className, String agentName) {
		Logger logger = Logger.getJADELogger(className + '.' + agentName);
		logger.setLevel(Logger.ALL);
		
		try {
			FileHandler fh = new FileHandler(LOG_PATH + agentName + ".log");
			fh.setFormatter(messageFormatter());
			logger.addHandler(fh);
			
			StreamHandler sh = new StreamHandler(System.out, messageFormatter());
			sh.setLevel(Level.ALL);
			logger.addHandler(sh);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		
		return logger;
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
	