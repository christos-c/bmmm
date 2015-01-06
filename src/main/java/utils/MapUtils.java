package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapUtils {
	
	public static <K, V> List<K> sortByValueList(final Map<K, V> m) {
		List<K> keys = new ArrayList<K>();
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
	
	/** Return an ordered list containing only items above the threshold. */
	public static List<String> sortByValueList(final Map m, double percentage) {
		List<String> keys = new ArrayList<String>();
		List<String> keysThresh = new ArrayList<String>();
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
		double maxVal, thresh, value;
		if (m.get(keys.get(0)) instanceof Integer)
			maxVal = (Integer) m.get(keys.get(0));
		else maxVal = (Double) m.get(keys.get(0));
		thresh = maxVal*percentage;
		for (String key:keys){
			if (m.get(key) instanceof Integer)
				value = (Integer) m.get(key);
			else value = (Double) m.get(key);
			if (value<thresh) break;
			keysThresh.add(key);
		}
		return keysThresh;
	}
	
	public static <K, V> Map<K, V> sortByValueMap(final Map<K, V> m) {
		List<K> keys = new ArrayList<K>();
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
		Map<K, V> result = new LinkedHashMap<K, V>();
		for (K key : keys) result.put(key, m.get(key));
		return result;
	}

	public static List<Map<String, Double>> MapList2ListMap(Map<String, List<Double>> mapList){
		List<Map<String, Double>> listMap = new ArrayList<Map<String, Double>>();
		
		int combinations = 1;
		for (String k:mapList.keySet()){
			combinations*=mapList.get(k).size();
		}
		
		List<List<Double>> bigList = new ArrayList<List<Double>>();
		for (List<Double> val:mapList.values()){
			bigList.add(val);
		}
		List<String> keys = new ArrayList<String>();
		for (String key:mapList.keySet()){
			keys.add(key);
		}
		int index[] = new int[bigList.size()];
		Arrays.fill(index, 0);
		
		//First combo
		Map<String, Double> tempMap = new HashMap<String, Double>();
		for (int i = 0; i < keys.size(); i++){
			tempMap.put(keys.get(i), bigList.get(i).get(index[i]));
		}
		listMap.add(new HashMap<String,Double>(tempMap));
		tempMap.clear();
		
		for (int j=0; j<combinations-1; j++){
			boolean found = false;
			// We use reverse order
			for (int l = index.length-1 ; (l >= 0) && !found; l--) {
				int currentListSize = bigList.get(l).size();
				if (index[l] < currentListSize-1) {
					index[l] = index[l]+1;
					found = true;
				}
				else {
					// Overflow
					index[l] = 0;
				}
			}
			for (int i = 0; i < keys.size(); i++){
				tempMap.put(keys.get(i), bigList.get(i).get(index[i]));
			}
			listMap.add(new HashMap<String,Double>(tempMap));
			tempMap.clear();
		}
		
		return listMap;
	}
	
	public static int sumMap(Map<String, Integer> map){
		int sum = 0;
		for (String key:map.keySet()) sum += map.get(key);
		return sum;
	}
}
