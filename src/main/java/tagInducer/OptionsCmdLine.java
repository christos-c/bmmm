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
			if (args[i].equals("-in")) {
				if (!checkNext(args,i)) corpusFileName = args[++i];
			}
			else if (args[i].equals("-out")) {
				if (!checkNext(args,i)) outFile = args[++i];
			}
			else if (args[i].equals("-iters")) {
				if (!checkNext(args,i)) numIters = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-classes")) {
				if (!checkNext(args,i)) numClasses = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-printLog")) {
				printLog = true;
			}
			else if (args[i].equals("-deps")) {
				if (!checkNext(args,i)) depsFile = args[++i];
				featureTypes.add(FeatureNames.DEPS);
			}
			else if (args[i].equals("-parg")) {
				if (!checkNext(args,i)) pargFile = args[++i];
				featureTypes.add(FeatureNames.PARG);
			}
			else if (args[i].equals("-morph")) {
				if (!checkNext(args,i)) morphFile = args[++i];
				featureTypes.add(FeatureNames.MORPH);
			}
			else if (args[i].equals("-noContext")){
				featureTypes.remove(FeatureNames.CONTEXT);
			}
			//Alignment options
			else if (args[i].equals("-alignLangs")){
				featureTypes.add(FeatureNames.ALIGNS);
				//Next argument should be either one language, or a list in ""
				if (!checkNext(args,i)) {
					String temp = args[++i];
					if (temp.contains(" ")) alignLangs = temp.split("\\s+");
					else {
						alignLangs = new String[1];
						alignLangs[0] = temp;
					}
				}
			}
			else if (args[i].equals("-alignFile")){
				if (!checkNext(args,i)) alignmentsFile = args[++i];
			}
			else if (args[i].equals("-langsFileRegexp")){
				if (!checkNext(args,i)) alignLangFileRegexp = args[++i];
			}
			else if (args[i].equals("-useLR")){
				alignUseLRContext = true;
			}
			else if (args[i].equals("-corpusLang")){
				if (!checkNext(args,i)) corpusLang = args[++i];
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
