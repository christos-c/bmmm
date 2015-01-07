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
				case "-printLog":
					printLog = true;
					break;
				case "-ignorePunct":
					ignorePunct = true;
					break;
				case "-deps":
					if (!checkNext(args, i)) depsFile = args[++i];
					featureTypes.add(FeatureNames.DEPS);
					break;
				case "-parg":
					if (!checkNext(args, i)) pargFile = args[++i];
					featureTypes.add(FeatureNames.PARG);
					break;
				case "-morph":
					if (!checkNext(args, i)) morphFile = args[++i];
					featureTypes.add(FeatureNames.MORPH);
					break;
				case "-noContext":
					featureTypes.remove(FeatureNames.CONTEXT);
					break;
				//Alignment options
				case "-alignLangs":
					featureTypes.add(FeatureNames.ALIGNS);
					//Next argument should be either one language, or a list in ""
					if (!checkNext(args, i)) {
						String temp = args[++i];
						if (temp.contains(" ")) alignLangs = temp.split("\\s+");
						else {
							alignLangs = new String[1];
							alignLangs[0] = temp;
						}
					}
					break;
				case "-alignFile":
					if (!checkNext(args, i)) alignmentsFile = args[++i];
					break;
				case "-langsFileRegexp":
					if (!checkNext(args, i)) alignLangFileRegexp = args[++i];
					break;
				case "-useLR":
					alignUseLRContext = true;
					break;
				case "-corpusLang":
					if (!checkNext(args, i)) corpusLang = args[++i];
					break;
			}
		}
	}
	
	private void setDefaults(){
		numClasses = 45;
		numContextFeats = 200;
		numIters = 1000;
		printLog = false;
		extendedMorph = true;
		featureTypes.add("CONTEXT");
		morphMethod = "morfessor";
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
		usage += "-iters <num>:\tNumber of iterations (default=1000)";
		usage += "\n\t";
		usage += "-classes <num>:\tNumber of classes (default=45)";
		usage += "\n\t";
		usage += "-printLog:\tPrint log (default=false)";
		usage += "\n\t";
		usage += "-ignorePunct:\tUse cluster ID -1 for all punctuation marks (default=false)";
		usage += "\n\t";
		usage += "## Feature options ##";
		usage += "\n\t";
		usage += "-deps [file]:\tUse dependency features (add file if corpus isn't CoNLL style)";
		usage += "\n\t";
		usage += "-parg [file]:\tUse PARG features";
		usage += "\n\t";
		usage += "-morph <file>:\tUse morfessor features";
		usage += "\n\t";
		usage += "-noContext:\tDo *not* use context features";
		usage += "\n\t";
		usage += "-alignLangs <lang> or \"lang1 lang2...\":\tUse alignment features";
		usage += "\n\t\t";
		usage += "-langsFileRegexp <regexp>:\tA regexp for language corpora in the form 'dir/*.ext'";
		usage += "\n\t\t";
		usage += "-useLR:\t\t\t\tUse the left/right context of the aligned word instead of the word itself";
		usage += "\n\t\t";
		usage += "-corpusLang <lang>:\t\tThe language of the source corpus";
		usage += "\n\t\t";
		usage += "-alignFile <file>:\t\tFile containing the word alignments";
		return usage;
	}
}
