package utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Utils {

	public static final String DEFAULT_BUYERS_PATH = "data/buyers.xml", DEFAULT_SELLERS_PATH = "data/sellers.xml";

	public static final String SD_BUY = "SD_BUY", SD_SELL = "SD_SELL", SIMULATOR_AGENT = "SIMULATOR_AGENT";

	public static final String PURCHASE = "PURCHASE", WAIT = "WAIT", CANCEL = "CANCEL";

	public static final String CFP_PROTOCOL = "FCFP", RATE = "RATE", NULL = "NULL";

	public static float round(float number, int scale) {
		int pow = 10;
		for (int i = 1; i < scale; i++)
			pow *= 10;
		float tmp = number * pow;
		return ((float) ((int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp))) / pow;
	}

	public static ConcurrentHashMap<String, Integer> sortHashMapByValues(ConcurrentHashMap<String, Integer> passedMap) {
	    List<String> mapKeys = new ArrayList<>(passedMap.keySet());
	    List<Integer> mapValues = new ArrayList<>(passedMap.values());
	    Collections.sort(mapValues);
	    Collections.sort(mapKeys);

	    ConcurrentHashMap<String, Integer> sortedMap = new ConcurrentHashMap<>();

	    Iterator<Integer> valueIt = mapValues.iterator();
	    while (valueIt.hasNext()) {
	        Integer val = valueIt.next();
	        Iterator<String> keyIt = mapKeys.iterator();

	        while (keyIt.hasNext()) {
	            String key = keyIt.next();
	            Integer comp1 = passedMap.get(key);
	            Integer comp2 = val;

	            if (comp1.equals(comp2)) {
	                keyIt.remove();
	                sortedMap.put(key, val);
	                break;
	            }
	        }
	    }
	    return sortedMap;
	}

}
