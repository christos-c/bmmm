package tagInducer.utils;

import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.*;
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

	public static File[] listFilesMatching(String regExp) {
		String dirString = ".";
		String pargRegExp = regExp;
		if (regExp.contains("/")) {
			dirString = regExp.substring(0, regExp.lastIndexOf('/'));
			pargRegExp = regExp.substring(regExp.lastIndexOf('/') + 1, regExp.length());
		}
		pargRegExp = pargRegExp.replace("?", ".?").replace("*", ".*?");
		File dir = new File(dirString);
		FileFilter fileFilter = new RegexFileFilter(pargRegExp);
		return dir.listFiles(fileFilter);
	}
}
