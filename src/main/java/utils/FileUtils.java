package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileUtils {
	
	private static final String enc = "UTF8";

	public static BufferedReader createIn(String fileStr) throws IOException {
		FileInputStream fis = new FileInputStream(new File(fileStr));
		InputStreamReader inStream;
		if (fileStr.endsWith(".gz")) {
			GZIPInputStream gis = new GZIPInputStream(fis);
			inStream = new InputStreamReader(gis, enc);
		}
		else inStream = new InputStreamReader(fis, enc);
		return new BufferedReader(inStream);
	}

	public static BufferedWriter createOut(String fileStr, boolean append) throws IOException {
		FileOutputStream fis = new FileOutputStream(new File(fileStr), append);
		OutputStreamWriter inStream = new OutputStreamWriter(fis, enc);
		return new BufferedWriter(inStream);
	}
	
	public static BufferedWriter createOut(String fileStr) throws IOException {
		FileOutputStream fos = new FileOutputStream(new File(fileStr), false);
		OutputStreamWriter outStream;
		if (fileStr.endsWith(".gz")) {
			GZIPOutputStream gos = new GZIPOutputStream(fos);
			outStream = new OutputStreamWriter(gos, enc);
		}
		else outStream = new OutputStreamWriter(fos, enc);
		return new BufferedWriter(outStream);
	}

	public static String strip(String file) {
		return file.substring(0,file.lastIndexOf('.'));
	}
}
