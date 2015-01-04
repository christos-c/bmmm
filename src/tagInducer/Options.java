package tagInducer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A storage class to keep all the sampler options.
 * @author Christos Christodoulopoulos
 */
public class Options {
	protected static Properties config = new Properties();
	protected int numClasses, numRuns, numContextFeats, numIters;
	protected boolean printLog, extendedMorph, alignUseLRContext;
	protected String morphMethod, untiedHyperType, morphFile, 
	alignmentsFile, corpusFileName, outFile, depsFile, 
	alignLangFileRegexp, corpusLang;
	protected String[] alignLangs;
	protected List<String> featureTypes = new ArrayList<String>();
	
	public Options(){}

	public Options(String configFile){
		String temp;
		try {
			FileInputStream in = new FileInputStream(configFile);
			config.load(in); 
		}
		catch (IOException  e) {
			System.err.println("Cannot read configuration file " + configFile);
			System.exit(1);
		}
		corpusFileName = config.getProperty("CORPUS");
		numClasses = Integer.parseInt(config.getProperty("NUM_CLUSTERS"));
		temp = config.getProperty("FEATURE_TYPES");
		if (temp != null){
			if (temp.contains("[")){
				String[] types = temp.substring(1, temp.length()-1).split("\\s+");
				for (String type:types) featureTypes.add(type.toUpperCase());
			}
			else featureTypes.add(temp.toUpperCase());
		}
		
		temp = config.getProperty("SAMPLE_ITERS");
		numIters = (Integer.parseInt(temp));
		untiedHyperType = config.getProperty("UNTIED_HYPER_TYPE");
		temp = config.getProperty("NUM_RUNS");
		if (temp != null) numRuns = Integer.parseInt(temp);
		temp = config.getProperty("NUM_CONTEXT_FEATS");
		if (temp != null) numContextFeats = Integer.parseInt(temp);
		printLog = Boolean.parseBoolean(config.getProperty("PRINT_LOG"));
		depsFile = config.getProperty("DEPS_FILE");
		
		//Morphology options
		morphMethod = config.getProperty("MORPH_METHOD");
		morphFile = config.getProperty("MORPH_FILE");
		extendedMorph = Boolean.parseBoolean(config.getProperty("EXTENDED_MORPH"));
		
		//Alignment options
		alignmentsFile = config.getProperty("ALIGNMENTS_FILE");
		temp = config.getProperty("OUT_FILE");
		if (temp != null) outFile = temp;
		temp = config.getProperty("ALIGNMENT_LANGS");
		if (temp != null){
			if (temp.contains("[")){
				alignLangs = temp.substring(1, temp.length()-1).split("\\s+");
			}
			else {
				alignLangs = new String[1];
				alignLangs[0] = temp;
			}
		}
		alignLangFileRegexp = config.getProperty("LANG_FILE_REGEXP");
		alignUseLRContext = Boolean.parseBoolean(config.getProperty("USE_LR_CONTEXT"));
		corpusLang = config.getProperty("CORPUS_LANG");
	}
	
	public int getNumClasses() {return numClasses;}
	public int getNumRuns() {return numRuns;}
	public int getNumContextFeats() {return numContextFeats;}
	public boolean isPrintLog() {return printLog;}
	public boolean isExtendedMorph() {return extendedMorph;}
	public String getMorphMethod() {return morphMethod;}
	public String getMorphFile() {return morphFile;}
	public String getDepsFile() {return depsFile;}
	public String getAlignmentsFile() {return alignmentsFile;}
	public String getCorpusFileName() {return corpusFileName;}
	public String[] getAlignLangs() {return alignLangs;}
	public int getIters() {return numIters;}
	public List<String> getFeatureTypes() {return featureTypes;}
	public String getTiedHyperType() {return untiedHyperType;}
	public boolean isOutputToSingleFile() {return outFile != null;}
	public String getOutFile() {return outFile;}
	public String getLangFileRegexp() {return alignLangFileRegexp;}
	public boolean useLRContextWords() {return alignUseLRContext;}
	public String getCorpusLanguage() {return corpusLang;}

	public static String usage() {
		String usage = "Usage:\n";
		usage += "1)With configuration file: \n>tagInducer.Inducer config.txt\n";
		usage += "\n";
		usage += "2)With command line options:\n"+OptionsCmdLine.usage();
		return usage;
	}
	
	public String toString(){
		String str = "## General Parameters:\n";
		str += "Features:\t";
		for (String feat:featureTypes) str += feat+" ";
		str = str.trim();
		str += "\n";
		str += "Corpus File:\t"+corpusFileName;
		str += "\n";
		str += "Num Iterations:\t"+numIters;
		str += "\n";
		str += "Num Classes:\t"+numClasses;
		str += "\n";
		if (featureTypes.contains("DEPS")) {
			str += "  ## Dependency Parameters:\n";
			str += "  --Deps File:\t"+depsFile;
			str += "\n";
		}
		if (featureTypes.contains("MORPH")) {
			str += "  ## Morphology Parameters:\n";
			str += "  --Morph Method:\t"+morphMethod;
			str += "\n";
			str += "  --Morph File:\t"+morphFile;
			str += "\n";
			str += "  --Ext. Morph:\t"+extendedMorph;
			str += "\n";
		}
		if (featureTypes.contains("ALIGNS")) {
			str += "  ## Alignment Parameters:\n";
			str += "  --Aligns File:\t"+alignmentsFile;
			str += "\n";
			str += "  --Corpus Lang:\t"+corpusLang;
			str += "\n";
			str += "  --Align Langs:\t[";
			for (String lang:alignLangs) str += lang+" ";
			str = str.trim()+"]";
			str += "\n";
			str += "  --Lang Corpora:\t[";
			for (String lang:alignLangs) str += alignLangFileRegexp.replaceAll("\\*", lang)+" ";
			str = str.trim()+"]";
			str += "\n";
			str += "  --LR context:\t"+alignUseLRContext;
			str += "\n";
		}
		return str;
	}
}
