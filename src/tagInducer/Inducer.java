package tagInducer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import tagInducer.corpus.Corpus;
import utils.TagStats;

/**
 * The entry-point class.
 * @author Christos Christodoulopoulos
 */
public class Inducer{
	private static String outDir = "results";
	private String outFile, runDir;
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
		
		//Parse the corpus
		corpus = new Corpus(o);
		
		if (featureTypes.contains("CONTEXT")) corpus.addAllContextFeats();
		if (featureTypes.contains("DEPS")) corpus.addDepFeats();
		if (featureTypes.contains("MORPH")) corpus.addMorphFeats();
		if (featureTypes.contains("ALIGNS")) corpus.addAlignsFeats();
		
		//Construct and configure the sampler
		sampler = new FullGibbsSampler(corpus, o);
		sampler.initialise(o.getNumClasses());

		for (int run = 1; run <= o.getNumRuns(); run++){
			int iters = o.getIters();
			System.out.println("[REP: "+run+"/"+o.getNumRuns()+"]");
			runInducer(iters);
			System.out.println();
		}

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
			if (o.getTiedHyperType()!=null) dName += "_"+o.getTiedHyperType();
			dName += "_i"+iters;

			int maxRun = 0;
			File dFile = new File(outDir+"/"+dName);
			if (dFile.isDirectory()){
				File[] subDirs = dFile.listFiles();
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
