package utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StringUtils {
	
	private String tagDel = "/";
	
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

	public String extractWord(String wordTag){
		String word;
		//Bug catch
		if (wordTag.equals("/")) return wordTag;
		if (wordTag.split(tagDel).length>2) {
			word = wordTag.split(tagDel)[0];
			for (int i=1; i<wordTag.split(tagDel).length-1; i++){
				word+=tagDel+wordTag.split(tagDel)[i];
			}
		}
		else {
			word = wordTag.split(tagDel)[0];
		}
		return word;
	}
	
	public String extractTag(String wordTag){
		if (wordTag.split(tagDel).length>2)
			return wordTag.split(tagDel)[wordTag.split(tagDel).length-1];
		else return wordTag.split(tagDel)[1];
	}

	public boolean checkString(String s, int len){
		if (s.length() <= len) return false;
		for (int ii = s.length(); ii --> 0; ) {
			if (!Character.isLetter(s.charAt(ii)))
				return false;
		}
		return true;
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
	public String del(String toDel){
		char bs = '\b';
		String d = "";
		for (int i = 0; i < toDel.toCharArray().length; i++) {
			d += bs;
		}
		return d;
	}
}
