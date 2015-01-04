package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileUtils {
	
	private String enc;

	public FileUtils(String encoding){
		enc=encoding;
	}
	
	public FileUtils(){
		enc="UTF8";
	}
	
	public BufferedReader createIn(File rFile) throws IOException{
		FileInputStream fis = new FileInputStream(rFile);
		InputStreamReader inStream = new InputStreamReader(fis, enc);
		return  new BufferedReader(inStream);
	}
	
	public BufferedReader createIn(String fileStr) throws IOException{
		FileInputStream fis = new FileInputStream(new File(fileStr));
		InputStreamReader inStream = new InputStreamReader(fis, enc);
		return  new BufferedReader(inStream);
	}
	
	public BufferedWriter createOut(File wFile, boolean append) throws IOException{
		FileOutputStream fis = new FileOutputStream(wFile, append);
		OutputStreamWriter inStream = new OutputStreamWriter(fis, enc);
		return  new BufferedWriter(inStream);
	}
	
	public BufferedWriter createOut(File wFile) throws IOException{
		FileOutputStream fis = new FileOutputStream(wFile, false);
		OutputStreamWriter inStream = new OutputStreamWriter(fis, enc);
		return  new BufferedWriter(inStream);
	}
	
	public BufferedWriter createOut(String fileStr, boolean append) throws IOException{
		FileOutputStream fis = new FileOutputStream(new File(fileStr), append);
		OutputStreamWriter inStream = new OutputStreamWriter(fis, enc);
		return  new BufferedWriter(inStream);
	}
	
	public BufferedWriter createOut(String fileStr) throws IOException{
		FileOutputStream fis = new FileOutputStream(new File(fileStr), false);
		OutputStreamWriter inStream = new OutputStreamWriter(fis, enc);
		return  new BufferedWriter(inStream);
	}

	public String strip(String file) {
		return file.substring(0,file.lastIndexOf('.'));
	}
}
