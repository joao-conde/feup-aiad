package utilities;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import jade.util.Logger;

public class MarketLogger {

	public static final String DEFAULT_LOG_PATH = "logs/";
	public static String logPath;
	
	public static void setLogPath(String _logPath) {
		logPath = DEFAULT_LOG_PATH + _logPath + '/';
	}
	
	public static void calculateLogPath() {
		File folder = new File(DEFAULT_LOG_PATH);
		File[] listOfFiles = folder.listFiles();
		Integer number = 0;
		
		if(!folder.exists()) {
			logPath = DEFAULT_LOG_PATH + "1/";
			return;
		}
		
		for(File subfolder: listOfFiles) {
			if(subfolder.isDirectory()) {
				
		        if(number < Integer.parseInt(subfolder.getName())) {
		        	number = Integer.parseInt(subfolder.getName());
		        }
			}
		}
		
		number++;
		logPath = DEFAULT_LOG_PATH + number.toString() + '/';
	}

	public static Logger createLogger(String className, String agentName) {
		Logger logger = Logger.getJADELogger(className + '.' + agentName);
		logger.setLevel(Logger.ALL);

		/*try {
			FileHandler fh = new FileHandler(logPath + agentName + ".log");
			fh.setFormatter(messageFormatter());
			logger.addHandler(fh);

			StreamHandler sh = buildSH();
			sh.setLevel(Level.ALL);
			logger.addHandler(sh);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
*/
		return logger;
	}
	
	public static StreamHandler buildSH() {
	    final StreamHandler sh = new StreamHandler(System.out, messageFormatter()) {
	        @Override
	        public synchronized void publish(final LogRecord record) {
	            super.publish(record);
	            flush();
	        }
	    };
	    return sh;
	}

	public static SimpleFormatter messageFormatter() {
		return new SimpleFormatter() {
			private static final String format = "[%1$tF %1$tT] %3$s %n";
			@Override
			public synchronized String format(LogRecord lr) {
				return String.format(format, new Date(lr.getMillis()), lr.getLevel().getLocalizedName(),
						lr.getMessage());
			}
			
			
		};
	}
}
