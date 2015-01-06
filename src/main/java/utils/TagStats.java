package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagStats {
	
	public void createConfusionMatrix(String sFile, String uFile) throws IOException{
		BufferedReader supIn = FileUtils.createIn(sFile);
		BufferedReader unsupIn = FileUtils.createIn(uFile);
		BufferedWriter out = FileUtils.createOut(FileUtils.strip(uFile) + ".confMatrix");
		String supLine, unsupLine, sTag, uTag, supWordTag, unsupWordTag;
		String[] supWordTags, unsupWordTags;
		Map<String, Integer> clustFreq = new HashMap<String, Integer>();
		Map<String, Integer> tagFreq = new HashMap<String, Integer>();
		Map<String, Integer> coocMap = new HashMap<String, Integer>();
		while ((supLine=supIn.readLine())!=null){
			unsupLine=unsupIn.readLine().trim();
			supWordTags = supLine.split("\\s+");
			unsupWordTags = unsupLine.split("\\s+");
			//Assume both lines are the same
			for (int i=0; i<supWordTags.length; i++){
				supWordTag = supWordTags[i];
				unsupWordTag = unsupWordTags[i];
				sTag = extractTag(supWordTag);
				uTag = extractTag(unsupWordTag);
				if (clustFreq.containsKey(uTag)) clustFreq.put(uTag, clustFreq.get(uTag)+1);
				else clustFreq.put(uTag, 1);
				if (tagFreq.containsKey(sTag)) tagFreq.put(sTag, tagFreq.get(sTag)+1);
				else tagFreq.put(sTag, 1);
				String key = sTag+"_"+uTag;
				if (coocMap.containsKey(key)) coocMap.put(key, coocMap.get(key)+1);
				else coocMap.put(key, 1);
			}
		}
		//Sort the maps
		List<String> tagsSorted = MapUtils.sortByValueList(tagFreq);
		List<String> clustSorted = MapUtils.sortByValueList(clustFreq);
		//Start with a blank column title
		out.write("\t");
		//First print the column titles
		for (String tag:tagsSorted){
			out.write(tag+"\t");
		}
		out.write(System.getProperty("line.separator"));
		
		for (String clust:clustSorted){
			//First print the row title
			out.write(clust+"\t");
			for (String tag:tagsSorted){
				if (coocMap.get(tag+"_"+clust) != null)
					out.write(coocMap.get(tag+"_"+clust)+"\t");
				else out.write("-\t");
			}
			//Print the cluster sum
			int value = clustFreq.get(clust);
			out.write(Integer.toString(value));
			out.write(System.getProperty("line.separator"));
		}
		out.write("\t");
		for (String tag:tagsSorted){
			out.write(tagFreq.get(tag)+"\t");
		}
		//Final line print the tag sums
		out.close();
	}
	
	/**
	 * Creates a list of cluster names and assigned words
	 * @param file The tagged file
	 * @throws IOException
	 */
	public void createClusters(String file, int topN) throws IOException{
		BufferedReader in = FileUtils.createIn(file);
		BufferedWriter out = FileUtils.createOut(FileUtils.strip(file) + ".clusters");
		String line, tag, word;
		String[] wordTags;
		Map<String, Map<String, Integer>> clusters = new HashMap<String, Map<String,Integer>>();
		while ((line=in.readLine())!=null){
			wordTags = line.split("\\s+");
			for (String wordTag:wordTags){
				word = extractWord(wordTag);
				tag = extractTag(wordTag);
				if (clusters.containsKey(tag)){
					if (!clusters.get(tag).containsKey(word)) clusters.get(tag).put(word,1);
					else clusters.get(tag).put(word,clusters.get(tag).get(word)+1);
				}
				else {
					Map<String, Integer> map = new HashMap<String, Integer>();
					map.put(word, 1);
					clusters.put(tag, map);
				}
			}
		}
		for (String key:clusters.keySet()){
			//Sort the map to word frequency
			List<String> list = MapUtils.sortByValueList(clusters.get(key));
			if (list.size()>topN) list = list.subList(0, topN);
			out.write(key+"\t"+list+System.getProperty("line.separator"));
		}
		out.close();
	}
	
	/**
	 * Creates a tag frequency distribution from a tagged file<br>
	 * and outputs a sorted list of tags with frequencies.
	 * @param file The tagged file
	 * @throws IOException
	 */
	public void createTagDist(String file) throws IOException{
		BufferedReader in = FileUtils.createIn(file);
		BufferedWriter out = FileUtils.createOut(FileUtils.strip(file) + ".tagDist");
		String line,tag;
		String[] wordTags;
		Map<String, Integer> freqMap = new HashMap<String, Integer>();
		while ((line=in.readLine())!=null){
			wordTags = line.split("\\s+");
			for (String wordTag:wordTags){
				tag = extractTag(wordTag);
				if (freqMap.containsKey(tag)) freqMap.put(tag, freqMap.get(tag)+1);
				else freqMap.put(tag, 1);
			}
		}
		freqMap = MapUtils.sortByValueMap(freqMap);
		for (String key:freqMap.keySet()){
			out.write(key+"\t"+freqMap.get(key)+System.getProperty("line.separator"));
		}
		out.close();
	}
	
	/**
	 * Creates a mapping from unsupervised clusters to gold-standard tags <br>
	 * and outputs them in order of mapping frequency.
	 * @param sFile The file containing the gold-standard tags
	 * @param uFile The unsupervised tagged file
	 * @throws IOException
	 */
	public void createTagMap(String sFile, String uFile) throws IOException{
		BufferedReader supIn = FileUtils.createIn(sFile);
		BufferedReader unsupIn = FileUtils.createIn(uFile);
		BufferedWriter out = FileUtils.createOut(FileUtils.strip(uFile) + ".tagMap");
		String supLine, unsupLine, supWordTag, unsupWordTag, sTag, uTag;
		String[] supWordTags, unsupWordTags;
		Map<String, List<String>> tagMap = new HashMap<String, List<String>>();
		while ((supLine=supIn.readLine())!=null){
			unsupLine=unsupIn.readLine().trim();
			supWordTags = supLine.split("\\s+");
			unsupWordTags = unsupLine.split("\\s+");
			for (int i=0; i<supWordTags.length; i++){
				supWordTag = supWordTags[i];
				unsupWordTag = unsupWordTags[i];
				sTag = extractTag(supWordTag);
				uTag = extractTag(unsupWordTag);
				if (tagMap.containsKey(uTag)){
					tagMap.get(uTag).add(sTag);
				}
				else {
					List<String> tmp = new ArrayList<String>(1);
					tmp.add(sTag);
					tagMap.put(uTag, tmp);
				}
			}
		}
		for (String key:tagMap.keySet()){
			//Find the most popular tags
			String prefTag = findPrefTag(tagMap.get(key));
			out.write(key+"\t"+purity(tagMap.get(key),prefTag)+"\n");
		}
		out.close();
		supIn.close();
		unsupIn.close();
	}
	
	private String purity(List<String> list, String prefTag) {
		List<String> seen = new ArrayList<String>();
		int totalTags = list.size();
		int correctTags = numTags(list,prefTag);
		int num;
		String text = prefTag+"["+correctTags+"/"+totalTags+" "+perc(correctTags,totalTags)+"]\t";
		for (String tag:list){
			if (!tag.equals(prefTag) && !seen.contains(tag)){
				num=numTags(list,tag);
				text+=tag+"["+num+"/"+totalTags+" "+perc(num,totalTags)+"]\t";
				seen.add(tag);
			}
		}
		return text;
	}
	
	private int numTags(List<String> list, String tag){
		int numTags = 0;
		for (String t:list){
			if (t.equals(tag)) numTags++;
		}
		return numTags;
	}
	
	private String perc(int i, int j) {
		double p = (double)i/(double)j*100;
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		return twoDForm.format(p);
	}
	
	private String findPrefTag(List<String> list) {
		Map<String, Integer> tmpMap = new HashMap<String, Integer>();
		for (String tag:list){
			if (tmpMap.containsKey(tag)) {
				int f = tmpMap.get(tag) +1;
				tmpMap.remove(tag);
				tmpMap.put(tag, f);
			}
			else tmpMap.put(tag, 1);
		}
		return MapUtils.sortByValueList(tmpMap).get(0);
	}
	
	private String extractWord(String wordTag){
		String del = "/";
		String word;
		if (wordTag.split(del).length>2) {
			word = wordTag.split(del)[0];
			for (int i=1; i<wordTag.split(del).length-1; i++){
				word+=del+wordTag.split(del)[i];
			}
		}
		else {
			word  = wordTag.split(del)[0];
		}
		return word;
	}
	
	private String extractTag(String wordTag){
		String del = "/";
		if (wordTag.split(del).length>2)
			return wordTag.split(del)[wordTag.split(del).length-1];
		else return wordTag.split(del)[1];
	}
	
	public static void main(String[] args){
		TagStats t = new TagStats();
		try {
			t.createTagDist("smallWSJ.45.tagged");
			t.createTagMap("data_wsj/smallWSJ.tagged", "smallWSJ.45.tagged");
			t.createConfusionMatrix("data_wsj/smallWSJ.tagged", "smallWSJ.45.tagged");
			t.createClusters("smallWSJ.45.tagged", 10);			
		}
		catch (IOException e) {e.printStackTrace();}
	}
}
