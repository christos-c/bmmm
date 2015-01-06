package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapUtils {
	
	public static <K, V> List<K> sortByValueList(final Map<K, V> m) {
		List<K> keys = new ArrayList<>();
		keys.addAll(m.keySet());
		Collections.sort(keys, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				if (m.get(o1) instanceof Integer){
					Integer v1 = (Integer) m.get(o1);
					Integer v2 = (Integer) m.get(o2);
					return -v1.compareTo(v2);
				}
				else {
					Double v1 = (Double) m.get(o1);
					Double v2 = (Double) m.get(o2);
					return -v1.compareTo(v2);
				}
			}
		});
		return keys;
	}

	public static <K, V> Map<K, V> sortByValueMap(final Map<K, V> m) {
		List<K> keys = new ArrayList<>();
		keys.addAll(m.keySet());
		Collections.sort(keys, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				if (m.get(o1) instanceof Integer){
					Integer v1 = (Integer) m.get(o1);
					Integer v2 = (Integer) m.get(o2);
					return -v1.compareTo(v2);
				}	
				else {
					Double v1 = (Double) m.get(o1);
					Double v2 = (Double) m.get(o2);
					return -v1.compareTo(v2);
				}
			}
		});
		Map<K, V> result = new LinkedHashMap<>();
		for (K key : keys) result.put(key, m.get(key));
		return result;
	}

	public static int sumMap(Map<?, Integer> map){
		int sum = 0;
		for (Object key:map.keySet()) sum += map.get(key);
		return sum;
	}
}
