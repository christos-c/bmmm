package tagInducer.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class StringUtils {
	private static String[] punctArray = new String[] {":", ",", ".", "?", "!", ";", "...", "…",
			//  Unidcode for new String("»"), new String("«"), new String("“"), new String("”"),
			String.valueOf(Character.toChars('\u00BB')),
			String.valueOf(Character.toChars('\u00AB')),
			String.valueOf(Character.toChars('\u201C')),
			String.valueOf(Character.toChars('\u201D')),
			"(", ")",
			"{", "}",
			"[", "]",
			"<", ">",
			"-", "--",
			"``", "\'\'",
			"`", "'",
			"\""};
	private static List<String> punct = Arrays.asList(punctArray);

	/**
	 * Counts the number of sentences in a document
	 */
	public int count(String filename) throws IOException {
	    InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    byte[] c = new byte[1024];
	    int count = 0;
	    int readChars;
	    while ((readChars = is.read(c)) != -1) {
	        for (int i = 0; i < readChars; ++i) {
	            if (c[i] == '\n')
	                ++count;
	        }
	    }
	    is.close();
	    return count;
	}

	public boolean checkString(String s, int len){
		if (s.length() <= len) return false;
		for (int ii = s.length(); ii --> 0; ) {
			if (!Character.isLetter(s.charAt(ii)))
				return false;
		}
		return true;
	}

	public static boolean isPunct(String s) {
		return punct.contains(s);
	}

	public String[] split(String str, int point){
		String[] result = new String[2];
		if (point >= str.length()) result[0] = str;
		else{
			result[0] = str.substring(0,point);
			result[1] = str.substring(point,str.length());
		}
		return result;
	}
	
	public String joinSuffixes(String[] splits) {
		String suff = ""; String newSuff;
		for (int i=splits.length-1; i>0; i--){
			if (!splits[i].contains("SUF")) break;
			newSuff = splits[i].substring(0,splits[i].indexOf('/')).trim();
			suff = newSuff.concat(suff);
		}
		return suff;
	}

	/** Helper function for pretty-printing progress (prints backspace characters) */
	public static String del(int numChars){
		String d = "";
		for (int i = 0; i < numChars; i++) d += '\b';
		return d;
	}
}
