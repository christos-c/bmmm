package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StringCoder {
	
	private Map<String, Integer> string2int;
	private Map<Integer, String> int2string;
	private int counter;

	public StringCoder(){
		string2int = new HashMap<>();
		int2string = new HashMap<>();
		counter = 0;
	}
	
	public int encode(String string){
		if (string2int.containsKey(string)) return string2int.get(string);
		string2int.put(string, counter);
		int2string.put(counter, string);
		counter++;
		return counter-1;
	}
	
	public String decode(int code){
		return int2string.get(code);
	}
	
	public int size(){
		//Should be equal to int2string
		return string2int.size();
	}

	public boolean exists(String word) {
		return string2int.containsKey(word);
	}

	public Set<Integer> intSet() {
		return int2string.keySet();
	}
	
	public Set<String> stringSet(){
		return string2int.keySet();
	}
}
