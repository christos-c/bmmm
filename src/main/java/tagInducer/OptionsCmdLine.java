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
					if (!checkNext(args, i)) corpusFileName = args[++i];
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
				case "-ignorePunct":
					ignorePunct = true;
					break;
				case "-lowercase":
					lowercase = true;
					break;
				case "-deps":
					featureTypes.add(FeatureNames.DEPS);
					break;
				case "-parg":
					if (!checkNext(args, i)) pargFile = args[++i];
					featureTypes.add(FeatureNames.PARG);
					break;
				case "-ccg-cats":
					featureTypes.add(FeatureNames.CCGCATS);
					break;
				case "-parg-deps":
					if (!checkNext(args, i)) pargFile = args[++i];
					featureTypes.add(FeatureNames.PARGDEPS);
					break;
				case "-morph":
					if (!checkNext(args, i)) morphFile = args[++i];
					featureTypes.add(FeatureNames.MORPH);
					break;
				case "-noContext":
					featureTypes.remove(FeatureNames.CONTEXT);
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
		extendedMorph = true;
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
		usage += "-in <file>:\tThe input corpus. Can be 1-sentence-per-line (tagged/raw) or CoNLL style";
		usage += "\n\t";
		usage += "-out <file>:\tThe output file. If not supplied the inducer with create an output dir";
		usage += "\n\t";
		usage += "-iters <num>:\tNumber of iterations (default=500)";
		usage += "\n\t";
		usage += "-classes <num>:\tNumber of classes (default=45)";
		usage += "\n\t";
		usage += "-ignorePunct:\tUse cluster ID -1 for all punctuation marks (default=false)";
		usage += "\n\t";
		usage += "-lowercase:\tLowercase the word types to be clustered, but not the actual corpus (default=false)";
		usage += "\n\t";
		usage += "## Feature options ##";
		usage += "\n\t";
		usage += "-deps:\tUse dependency features (read from input corpus)";
		usage += "\n\t";
		usage += "-ccg-cats:\tUse CCG category features (read from input corpus)";
		usage += "\n\t";
		usage += "-parg <file>:\tUse CCG PARG features";
		usage += "\n\t";
		usage += "-parg-deps <file>:\tUse CCG PARG dependency features";
		usage += "\n\t";
		usage += "-morph <file>:\tUse morfessor features";
		usage += "\n\t";
		usage += "-noContext:\tDo *not* use context features";
		usage += "\n\t";
		return usage;
	}
}
