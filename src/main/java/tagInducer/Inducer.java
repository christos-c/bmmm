package tagInducer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import tagInducer.corpus.CoNLLCorpus;
import tagInducer.corpus.Corpus;
import tagInducer.corpus.LineCorpus;
import tagInducer.features.FeatureNames;
import utils.FileUtils;
import utils.TagStats;

/**
 * The entry-point class.
 * @author Christos Christodoulopoulos
 */
public class Inducer{
	private static final String outDir = "results";
	private String runDir;
	private Corpus corpus;
	private Options o;
	private GibbsSampler sampler;
	private List<String> featureTypes;

	public Inducer(String[] args) throws IOException{
		if (args.length > 1) o = new OptionsCmdLine(args);
		else if (args.length == 1) o = new Options(args[0]);
		else {
			System.err.println(Options.usage());
			System.exit(1);
		}
		System.out.println(o);
		featureTypes = o.getFeatureTypes();

		//Read the corpus (need to determine if it's CoNLL-style or not)
		BufferedReader in = FileUtils.createIn(o.getCorpusFileName());
		String line = in.readLine();
		if (line.split("\\s{2,}").length > 1)
			corpus = new CoNLLCorpus(o);
		else corpus = new LineCorpus(o);

		if (featureTypes.contains(FeatureNames.CONTEXT)) corpus.addAllContextFeats();
		if (featureTypes.contains(FeatureNames.DEPS)) corpus.addDepFeats();
		if (featureTypes.contains(FeatureNames.MORPH)) corpus.addMorphFeats();
		if (featureTypes.contains(FeatureNames.ALIGNS)) corpus.addAlignsFeats();
		if (featureTypes.contains(FeatureNames.PARG)) corpus.addPargFeats();

		//Construct and configure the sampler
		sampler = new GibbsSampler(corpus, o);
		sampler.initialise(o.getNumClasses());

		int iters = o.getIters();
		long start = System.currentTimeMillis();
		runInducer(iters);
		long end = System.currentTimeMillis();
		System.out.println("Experiment took " + ((end - start) / 1000) + " seconds");
	}

	private void runInducer(int iters) throws IOException{
		String corpusFileName = o.getCorpusFileName();
		int numClusters = o.getNumClasses();

		//Check if the user specified languages for the align feature
		if (featureTypes.contains("ALIGNS")){
			if (o.getAlignLangs() == null) {
				System.err.println("Aligns feature types chosen without alignment languages!");
				System.exit(1);
			}
		}

		//IF the tagged generates all the output files, create the dir
		if (!o.isOutputToSingleFile()){
			//Generate the name of the directory for this run (dName)
			String cName = corpusFileName.substring(corpusFileName.lastIndexOf('/')+1,
					corpusFileName.lastIndexOf('.'));
			String dName = "";
			dName += "c"+numClusters;
			//Add the run's config
			for (String type:featureTypes) {
				if (type.equals("ALIGNS")){
					dName += "_aligns";
					for (String lang:o.getAlignLangs()) dName +=lang.toUpperCase();
				}
				else if (type.equals("MORPH")) {
					if (o.getMorphMethod().equalsIgnoreCase("morfessor")) dName += "_morphM";
					else if (o.getMorphMethod().equalsIgnoreCase("letter")) dName += "_morphL";
					if (o.isExtendedMorph()) dName+="_extMFeats";
				}
				else {
					dName += "_"+type;
				}
			}
			dName += "_i"+iters;

			int maxRun = 0;
			File dFile = new File(outDir+"/"+dName);
			if (dFile.isDirectory()){
				File[] subDirs = dFile.listFiles();
				assert subDirs != null;
				for (File subDir:subDirs){
					String sDirName = subDir.getName();
					if (sDirName.startsWith(cName)){
						int run = Integer.parseInt(sDirName.substring(
								sDirName.lastIndexOf('_')+1,sDirName.length()));
						if (run > maxRun) maxRun = run;
					}
				}
			}
			maxRun++;
			dName += "/"+cName+"_"+maxRun;

			//Create the output dir for this run
			runDir = outDir+"/"+dName;
			if (!new File(runDir).mkdirs()) {
				System.err.println("Couldn't create output dirs");
				System.exit(1);
			}
		}

		//Set the parameters for the current run
		sampler.setRunParameters(iters);

		//**Run the sampler!**
		//Dir can be null at this stage (if only tagged file is generated)
		sampler.gibbs(runDir);

		//Output the tagged file
		String outFile;
		if (o.isOutputToSingleFile())
			outFile = o.getOutFile();
		else outFile = runDir+"/out.tagged";
		corpus.writeTagged(sampler.getTokenAssignments(true), outFile);

		//Do some statistics (again only if we're generating all the files)
		if (!o.isOutputToSingleFile() && corpus.hasTags()){
			TagStats stat = new TagStats();
			stat.createTagDist(outFile);
			stat.createTagMap(corpusFileName, outFile);
			stat.createConfusionMatrix(corpusFileName, outFile);
			stat.createClusters(outFile, 20);
		}
	}

	public static void main(String[] args) {
		try{new Inducer(args);}
		catch(IOException e){e.printStackTrace();}
	}
}
