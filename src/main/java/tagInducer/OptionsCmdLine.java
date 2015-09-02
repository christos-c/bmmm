package tagInducer;


import tagInducer.features.FeatureNames;

public class OptionsCmdLine extends Options{

	/**
	 * Constructor for the command-line interface.
	 * Reads from stdin.
	 * @param args The command-line parameters
	 */
	public OptionsCmdLine(String[] args){
		setDefaults();
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-in":
					if (!checkNext(args, i)) jsonFileName = args[++i];
					break;
				case "-out":
					if (!checkNext(args, i)) outFile = args[++i];
					break;
				case "-iters":
					if (!checkNext(args, i)) numIters = Integer.parseInt(args[++i]);
					break;
				case "-classes":
					if (!checkNext(args, i)) numClasses = Integer.parseInt(args[++i]);
					break;
                case "-maxLength":
                    if (!checkNext(args, i)) maxLength = Integer.parseInt(args[++i]);
                    break;
				case "-context-feats":
					if (!checkNext(args, i)) numContextFeats = Integer.parseInt(args[++i]);
					break;
				case "-ignorePunct":
					ignorePunct = true;
					break;
				case "-lowercase":
					lowercase = true;
					break;
				case "-deps":
					if (!checkNext(args, i) && args[++i].equals("undir")) undirDeps = true;
					featureTypes.add(FeatureNames.DEPS);
					break;
				case "-parg":
					if (!checkNext(args, i)) pargFeatType = args[++i];
					featureTypes.add(FeatureNames.PARG);
					break;
				case "-ccg-cats":
					featureTypes.add(FeatureNames.CCGCATS);
					break;
				case "-morph":
					if (!checkNext(args, i)) morphFile = args[++i];
					featureTypes.add(FeatureNames.MORPH);
					break;
				case "-noContext":
					featureTypes.remove(FeatureNames.CONTEXT);
					break;
                case "-genDistr":
                    generateDistributions = true;
                    break;
				case "-api-key":
					if (!checkNext(args, i)) apiKeyFile = args[++i];
					break;
			}
		}
	}
	
	private void setDefaults(){
		numClasses = 45;
		numContextFeats = 100;
		numIters = 500;
		ignorePunct = false;
		lowercase = false;
		undirDeps = false;
		extendedMorph = true;
        maxLength = Integer.MAX_VALUE;
		featureTypes.add(FeatureNames.CONTEXT);
	}
	
	private boolean checkNext(String[] args, int i) {
		return args.length <= i + 1 || args[i + 1].startsWith("-");
	}
	
	public static String usage(){
		String usage = "$>tagInducer.Inducer -in <file> [options]";
		usage += "\n\t";
		usage += "## General options ##";
		usage += "\n\t";
		usage += "-in <file>:\tThe input corpus. It needs to be a JSON-formatted file";
		usage += "\n\t";
		usage += "-out <file>:\tThe output file";
		usage += "\n\t";
		usage += "-iters <num>:\tNumber of iterations (default=500)";
		usage += "\n\t";
		usage += "-classes <num>:\tNumber of classes (default=45)";
		usage += "\n\t";
		usage += "-ignorePunct:\tUse cluster ID -1 for all punctuation marks (default=false)";
		usage += "\n\t";
		usage += "-lowercase:\tLowercase the word types to be clustered, but not the actual corpus (default=false)";
		usage += "\n\t";
		usage += "-api-key <file>:\tSend a notification to android devices using this API key file";
		usage += "\n\t";
		usage += "## Feature options ##";
		usage += "\n\t";
		usage += "-context-feats <num>:\tNumber of context features (default=100)";
		usage += "\n\t";
		usage += "-deps [undir]:\tUse dependency features. undir: Allow undirected dependency features (default=false)";
		usage += "\n\t";
		usage += "-ccg-cats:\tUse CCG category features (read from input corpus)";
		usage += "\n\t";
		usage += "-parg [type]:\tUse CCG PARG features. type=[headcat|cat|context|all] (default=all)";
		usage += "\n\t";
		usage += "-parg-deps <regexp>:\tUse CCG PARG dependency features";
		usage += "\n\t";
		usage += "[-undir:\tAllow undirected dependency features (default=false)]";
		usage += "\n\t";
		usage += "-morph <file>:\tUse morfessor features";
		usage += "\n\t";
		usage += "-noContext:\tDo *not* use context features";
		usage += "\n\t";
        usage += "-genDistr:\tGenerate the distributions over classes for each type";
        usage += "\n\t";
		return usage;
	}
}
