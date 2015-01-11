package tagInducer.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class SimpleTokeniser {
	private List<String> punctMarks; 

	public SimpleTokeniser(){
		punctMarks = new ArrayList<>();
		punctMarks.add(".");
		punctMarks.add(",");
		punctMarks.add(";");
		punctMarks.add(")");
		punctMarks.add(":");
		punctMarks.add("?");
		punctMarks.add("!");
		punctMarks.add("'");
	}
	
	public String tokenise(String line) {
		//Find all punctuation marks and add a space before
		String endStr=line;
		for (String p:punctMarks){
			if (endStr.indexOf(p)>0) {
				endStr = addSpace(endStr, p);
			}
		}
		if (endStr.indexOf('(')>0){
			endStr = addSpaceBR(endStr);
		}
		return endStr+" .";
	}

	private String addSpaceBR(String line) {
		String endStr="";
		//Find all punctuation marks and add a space before
		String[] dotSplit = line.split("[(]");
		for (int i=0; i<dotSplit.length-1; i++){
			endStr += dotSplit[i] + "( ";
		}
		endStr+=dotSplit[dotSplit.length-1];
		return endStr;
	}
	
	private String addSpace(String line, String token){
		String endStr="";
		//Find all punctuation marks and add a space before
		String[] dotSplit = line.split("\\"+token);
		for (int i=0; i<dotSplit.length-1; i++){
			endStr += dotSplit[i] + " "+token;
		}
		endStr+=dotSplit[dotSplit.length-1];
		return endStr;
	}
	
	public static void main(String[] args){
		try{
			FileInputStream fis = new FileInputStream(new File(args[0]));
			InputStreamReader inStream = new InputStreamReader(fis, "UTF8");
			BufferedReader in = new BufferedReader(inStream);
			FileOutputStream fos = new FileOutputStream(new File(args[0]+".tok"), false);
			OutputStreamWriter outStream = new OutputStreamWriter(fos, "UTF8");
			BufferedWriter out =  new BufferedWriter(outStream);
			String line;
			SimpleTokeniser tok = new SimpleTokeniser();
			
			while ((line=in.readLine())!=null){
				out.write(tok.tokenise(line)+System.getProperty("line.separator"));
			}
			in.close();
			out.close();
		}
		catch(IOException e){e.printStackTrace();}
	}
}
