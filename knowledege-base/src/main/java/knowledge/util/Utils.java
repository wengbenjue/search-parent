package knowledge.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by soledede.weng on 2016/9/2.
 */
public class Utils {
    public static Map<String, Object> getSystemProperties() {
        Set<String> pros = System.getProperties().stringPropertyNames();
        Map<String, Object> result = new HashMap<>();
        Iterator<String> it = pros.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Object value = System.getProperty(key);
            result.put(key, value);
        }
        return result;
    }
}
