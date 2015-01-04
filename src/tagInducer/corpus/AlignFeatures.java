package tagInducer.corpus;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tagInducer.Options;
import utils.FileUtils;
import utils.MapUtils;
import utils.StringUtils;

public class AlignFeatures {
	
	private FileUtils f = new FileUtils();
	private StringUtils s = new StringUtils();
	
	/**
	 * The number of words in the other languages used as features 
	 * for the word types in this language.
	 * The same number is used either for the case of left/right words 
	 * or for the other language's word itself. 
	 */
	private static final int NUM_CONTEXT_WORDS = 100;

	public AlignFeatures(int numWordTypes, int[][] corpus, Map<String, int[][]> featureVectors,
			int[] words, Options o) throws IOException{
		String[] langs = o.getAlignLangs();
		String alignsFile = o.getAlignmentsFile();		
		String langFileRegexp = o.getLangFileRegexp();
		String corpusLang = o.getCorpusLanguage();
		boolean useLRContextWords = o.useLRContextWords();
		
		Map<String, int[][]> alignFeatures = new HashMap<String, int[][]>();
		Map<String, int[][]> corpora = new HashMap<String, int[][]>();
		Map<String, Map<Integer, Integer>> corporaContextWordMaps = new HashMap<String, Map<Integer, Integer>>();
		for (String lang:langs){
			int[][] features = new int[numWordTypes][NUM_CONTEXT_WORDS];
			alignFeatures.put(lang, features);
			
			int[][] temp = readLangCorpus(langFileRegexp, lang);
			corpora.put(lang, temp);
			Map<Integer, Integer> wordFreq = new HashMap<Integer, Integer>();
			//Get the word counts
			for (int[] sentence:temp){
				for (int word:sentence){
					if (wordFreq.containsKey(word)) wordFreq.put(word, wordFreq.get(word)+1);
					else wordFreq.put(word, 1);
				}
			}
			//Resort wrt frequency and prune
			List<Integer> contextWordList= MapUtils.sortByValueList(wordFreq).subList(0, NUM_CONTEXT_WORDS);
			Map<Integer, Integer> contextWordMap = new HashMap<Integer, Integer>();
			int wordInd = 0;
			for (int word:contextWordList){
				contextWordMap.put(word, wordInd);
				wordInd++;
			}
			corporaContextWordMaps.put(lang, contextWordMap);
		}

		BufferedReader in = f.createIn(alignsFile);
		String line, lang;
		int srcWordTypeInd, trgWordTypeInd, trgPrevWordTypeId, trgNextWordTypeId;
		String[] splits;
		BigWhile: while ((line = in.readLine()) != null){
			//Assume lang1.sentNo|wordNo-lang2.sentNo|wordNo
			if (!line.matches("[A-Za-z]+.[0-9]+\\|[0-9]+-[A-Za-z]+.[0-9]+\\|[0-9]+")){
				System.err.println("Alignments files doesn't contain the appropriate format:");
				System.err.println("lang1.sentNo|wordNo-lang2.sentNo|wordNo");
				System.exit(1);
			}
			if (!line.contains(corpusLang)) continue;
			for (String tempLang:langs) 
				if (!line.contains(tempLang)) continue BigWhile;

			trgPrevWordTypeId = -1; trgNextWordTypeId = -1;
			splits = line.split("\\W");
			int srcSentInd, srcWordInd, trgSentInd, trgWordInd;
			if (splits[0].equals(corpusLang)){
				lang = splits[3];
				srcSentInd = Integer.parseInt(splits[1]);
				srcWordInd = Integer.parseInt(splits[2]);
				trgSentInd = Integer.parseInt(splits[4]);
				trgWordInd = Integer.parseInt(splits[5]);
			}
			else {
				lang = splits[0];
				srcSentInd = Integer.parseInt(splits[4]);
				srcWordInd = Integer.parseInt(splits[5]);
				trgSentInd = Integer.parseInt(splits[1]);
				trgWordInd = Integer.parseInt(splits[2]);
			}
			srcWordTypeInd = corpus[srcSentInd][srcWordInd];
			//Catch some nasty alignment errors
			if (corpora.get(lang)[trgSentInd].length <= trgWordInd) continue;
			
			Map<Integer, Integer> langContextMap = corporaContextWordMaps.get(lang);
			int[][] langFeats = alignFeatures.get(lang);
			int[][] langCorpus = corpora.get(lang);
			if (useLRContextWords){
				if (trgWordInd-1 > 0)
					trgPrevWordTypeId = langCorpus[trgSentInd][trgWordInd-1];
				if (langCorpus[trgSentInd].length < trgWordInd+1)
					trgNextWordTypeId = langCorpus[trgSentInd][trgWordInd+1];

				if (langContextMap.containsKey(trgPrevWordTypeId) && trgPrevWordTypeId!=-1) 
					langFeats[srcWordTypeInd][langContextMap.get(trgPrevWordTypeId)]++;
				if (langContextMap.containsKey(trgNextWordTypeId) && trgNextWordTypeId!=-1) 
					langFeats[srcWordTypeInd][langContextMap.get(trgNextWordTypeId)]++;
			}
			else {
				trgWordTypeInd = langCorpus[trgSentInd][trgWordInd];
				if (langContextMap.containsKey(trgWordTypeInd) && trgWordTypeInd!=-1) 
					langFeats[srcWordTypeInd][langContextMap.get(trgWordTypeInd)]++;
			}
		}
		
		//Get the number of tokens per type
		int[] numTokens = new int[numWordTypes];
		for (int word:words) numTokens[word]++;
		
		//Account for one NULL feature for all languages
		int numTotFeats = NUM_CONTEXT_WORDS*alignFeatures.size();
		int features[][] = new int[numWordTypes][numTotFeats+1];
		int curPos = 0;
		int[] nullFeats = new int[numWordTypes];
		for (String language:corporaContextWordMaps.keySet()){
			int numAlignFeats = corporaContextWordMaps.get(language).size();
			int temp[][] = new int[numWordTypes][numAlignFeats+1];
			for (int wordType = 0; wordType < numWordTypes; wordType++) {
				for (int feat = 0; feat < numAlignFeats; feat++){
					temp[wordType][feat] = alignFeatures.get(language)[wordType][feat];
				}
				//Cumulate NULL features for each language (they are emitted every time no alignment feature is generated)
				//Effectively we take the number of tokens (sum of context features) and subtract the number of align features emitted
				int tokensPerType = numTokens[wordType];
				int alignTokens = sumAlignFeats(temp[wordType]);
				assert (tokensPerType >= alignTokens);
				nullFeats[wordType] += tokensPerType-alignTokens;
			}
			for (int i = 0; i < temp.length; i++) System.arraycopy(temp[i], 0, features[i], curPos, temp[i].length);
			curPos += temp[0].length;
		}
		//Add the total NULL feature emissions as the last element
		for (int wordType = 0; wordType < numWordTypes; wordType++)
			features[wordType][numTotFeats] = nullFeats[wordType];
		featureVectors.put("ALIGNS", features);
	}

	private int sumAlignFeats(int[] vector) {
		int sum = 0;
		for (int feat : vector) sum += feat;
		return sum;
	}

	public int[][] readLangCorpus(String regexp, String lang) throws IOException{
		String line;
		int wordTypeInd = 0, sentenceInd = 0;
		Map<String, Integer> wordTypes2Int = new HashMap<String, Integer>();
		
		String file = regexp.replaceAll("\\*", lang);
		file = file.substring(1, file.length()-1);

		int[][] corpus = new int[s.count(file)][];

		//Read the corpus
		BufferedReader in = f.createIn(file);
		while ((line = in.readLine())!=null){
			//Map word types to integers
			int[] lineWords = new int[line.split("\\s+").length];
			int wordInd = 0;
			for (String word:line.split("\\s+")){
				word = s.extractWord(word);
				//Get the word index
				if (!wordTypes2Int.containsKey(word)) {
					wordTypes2Int.put(word, wordTypeInd);
					wordTypeInd++;
				}
				lineWords[wordInd] = wordTypes2Int.get(word);
				wordInd++;
			}
			corpus[sentenceInd] = lineWords;
			sentenceInd++;
		}
		in.close();

		return corpus;
	}
}
