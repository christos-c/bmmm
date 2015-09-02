package tagInducer;

import tagInducer.features.FeatureNames;

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
	private static Properties config = new Properties();
	protected int numClasses, numContextFeats, numIters, maxLength;
	protected boolean extendedMorph, ignorePunct, lowercase, undirDeps, generateDistributions;
	protected String morphFile, pargFeatType, jsonFileName, outFile, apiKeyFile;
	protected List<String> featureTypes = new ArrayList<>();
	
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
		outFile = config.getProperty("OUT_FILE");
		jsonFileName = config.getProperty("CORPUS");
		numClasses = Integer.parseInt(config.getProperty("NUM_CLUSTERS"));
		temp = config.getProperty("FEATURE_TYPES");
		if (temp != null){
			if (temp.contains("[")){
				String[] types = temp.substring(1, temp.length()-1).split("\\s+");
				for (String type : types) parseFeatureType(type);
			}
			else parseFeatureType(temp);
		}
		
		temp = config.getProperty("SAMPLE_ITERS");
		numIters = (Integer.parseInt(temp));
		// Default is 100 most frequent words
		temp = config.getProperty("NUM_CONTEXT_FEATS", "100");
		numContextFeats = Integer.parseInt(temp);

		ignorePunct = Boolean.parseBoolean(config.getProperty("IGNORE_PUNCT"));
		lowercase = Boolean.parseBoolean(config.getProperty("LOWERCASE"));
		undirDeps = Boolean.parseBoolean(config.getProperty("UNDIR_DEPS"));

		// PARG options (default type = all)
		pargFeatType = config.getProperty("PARG_FEAT_TYPE", "all");
		
		// Morphology options
		morphFile = config.getProperty("MORPH_FILE");
		extendedMorph = Boolean.parseBoolean(config.getProperty("EXTENDED_MORPH"));

        if (config.contains("MAX_SENT_LENGTH"))
            maxLength = Integer.parseInt(config.getProperty("MAX_SENT_LENGTH"));
        else maxLength = Integer.MAX_VALUE;

		apiKeyFile = config.getProperty("API_KEY");

        // Whether to generate a per-class distribution for each type
        generateDistributions = Boolean.parseBoolean(config.getProperty("GENERATE_DISTR", "false"));
	}
	
	public int getNumClasses() {return numClasses;}
	public int getNumContextFeats() {return numContextFeats;}
	public boolean isExtendedMorph() {return extendedMorph;}
	public String getMorphFile() {return morphFile;}
	public String getPargFeatType() {return pargFeatType;}
	public String getCorpusFileName() {return jsonFileName;}
	public int getIters() {return numIters;}
	public List<String> getFeatureTypes() {return featureTypes;}
	public String getOutFile() {return outFile;}
	public boolean isIgnorePunct() {return ignorePunct;}
	public boolean isLowercase(){return lowercase;}
	public boolean isUndirDeps(){return undirDeps;}
	public String getAPIKeyFile(){return apiKeyFile;}
    public int getMaxLength() {return maxLength;}
    public boolean generateDistributions() {return generateDistributions;}

    public void setLowercase(boolean lowercase) {
		this.lowercase = lowercase;
	}

	public void setIgnorePunct(boolean ignorePunct) {
		this.ignorePunct = ignorePunct;
	}

	public static String usage() {
		String usage = "Usage:\n";
		usage += "1)With configuration file: \n$>tagInducer.Inducer config/bmmm.properties\n";
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
		str += "Corpus File:\t"+jsonFileName;
		str += "\n";
		str += "Num Iterations:\t"+numIters;
		str += "\n";
		str += "Num Classes:\t"+numClasses;
		str += "\n";
        str += "Max Sent. Length:\t" + ((maxLength == Integer.MAX_VALUE)? "Infinity": maxLength);
        str += "\n";
		str += "Ignore Punct.:\t"+ignorePunct;
		str += "\n";
		str += "Lowercasing:\t"+lowercase;
		str += "\n";
        if (generateDistributions) {
            str += "Generating per-type class distributions";
            str += "\n";
        }
		if (featureTypes.contains(FeatureNames.CONTEXT)) {
			str += "  ## Context Parameters:\n";
			str += "  --Cont. Words:\t"+numContextFeats;
			str += "\n";
		}
		if (featureTypes.contains(FeatureNames.DEPS)) {
			str += "  ## Dependency Parameters:\n";
			str += "  --Use undir:\t"+undirDeps;
			str += "\n";
		}
		if (featureTypes.contains(FeatureNames.CCGCATS)) {
			str += "  ## CCG-CATS Parameters:\n";
			str += "\n";
		}
		if (featureTypes.contains(FeatureNames.PARG)) {
			str += "  ## PARG Parameters:\n";
            str += "  --Type:\t"+pargFeatType;
			str += "\n";
            str += "  --Cont. Words:\t"+numContextFeats;
			str += "\n";
		}
		if (featureTypes.contains(FeatureNames.MORPH)) {
			str += "  ## Morphology Parameters:\n";
			str += "  --Morph File:\t"+morphFile;
			str += "\n";
			str += "  --Ext. Morph:\t"+extendedMorph;
			str += "\n";
		}
		return str;
	}

	/**
	 * Processes the raw feature string and adds the relevant feature type
	 * @param featString can be 'deps', 'morph', 'parg', 'parg-deps'
	 */
	private void parseFeatureType(String featString) {
		switch (featString) {
			case "context":
				featureTypes.add(FeatureNames.CONTEXT);
				break;
			case "morph":
				featureTypes.add(FeatureNames.MORPH);
				break;
			case "deps":
				featureTypes.add(FeatureNames.DEPS);
				break;
			case "parg":
				featureTypes.add(FeatureNames.PARG);
				break;
			case "ccg-cats":
				featureTypes.add(FeatureNames.CCGCATS);
				break;
			default:
				System.err.println("Feature type: " + featString + " not recognised");
		}
	}
}
