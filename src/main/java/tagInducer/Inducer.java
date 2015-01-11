package tagInducer;

import tagInducer.corpus.Corpus;
import tagInducer.features.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The entry-point class.
 * @author Christos Christodoulopoulos
 */
public class Inducer{
	private Corpus corpus;
	private Options o;
	private GibbsSampler sampler;


	public Inducer(String[] args) throws IOException{
		if (args.length > 1) o = new OptionsCmdLine(args);
		else if (args.length == 1) o = new Options(args[0]);
		else {
			System.err.println(Options.usage());
			System.exit(1);
		}
		System.out.println(o);
		List<String> featureTypes = o.getFeatureTypes();

		corpus = new Corpus(o);

		System.out.println("Corpus:\t" + corpus.getNumSentences() + " sents\t" +
				corpus.getNumTokens() + " tokens\t" + corpus.getNumTypes() + " types.");

		// A map of feature vectors per word type.
		// String will contain the feature type and
		// int[][] is a MxF array of features per type.
		Map<String, int[][]> featureVectors = new HashMap<>();

		if (featureTypes.contains(FeatureNames.CONTEXT)) {
			Features features = new ContextFeatures(corpus, o.getNumContextFeats());
			featureVectors.put(FeatureNames.CONTEXT, features.getFeatures());
		}
		if (featureTypes.contains(FeatureNames.DEPS)) {
			Features features = new DepFeatures(corpus);
			featureVectors.put(FeatureNames.DEPS, features.getFeatures());
		}
		if (featureTypes.contains(FeatureNames.MORPH)) {
			Features features = new MorfFeatures(corpus, o.getMorphFile(), o.isExtendedMorph());
			featureVectors.put(FeatureNames.MORPH, features.getFeatures());
		}
		if (featureTypes.contains(FeatureNames.ALIGNS)) {
			System.err.println("Alignment features are not currently supported");
			System.exit(-1);
		}
		if (featureTypes.contains(FeatureNames.PARG)) {
			Features features = new PargFeatures(corpus, o.getPargFile());
			featureVectors.put(FeatureNames.PARG, features.getFeatures());
		}
		if (featureTypes.contains(FeatureNames.PARGDEPS)) {
			Features features = new PargDepFeatures(corpus, o.getPargFile());
			featureVectors.put(FeatureNames.PARGDEPS, features.getFeatures());
		}

		//Construct and configure the sampler
		sampler = new GibbsSampler(featureVectors, corpus.getNumTypes(), o);
		sampler.initialise(o.getNumClasses());

		long runTime = runInducer(o.getIters());
		System.out.println("Clustering took " + (runTime / 60000) + " minutes");
	}

	private long runInducer(int iters) throws IOException{
		//Set the parameters for the current run
		sampler.setRunParameters(iters);

		//**Run the sampler!**
		long start = System.currentTimeMillis();
		sampler.gibbs();
		long end = System.currentTimeMillis();

		// Run an evaluation at the end
		if (corpus.hasTags()) {
			System.out.println();
			corpus.setCorpusClusters(sampler.getFinalAssignment());
			Evaluator eval = new Evaluator(corpus, false);
			System.out.println("M-1: " + eval.manyToOne() + "\tVM: " + eval.VMeasure() + "\tVI: " + eval.VI());
		}

		// Output the final logLikelihood
		System.out.println("Final LL: " + sampler.getBestClassLogP());

		//Output the tagged file
		corpus.writeTagged(o.getOutFile());
		return (end - start);
	}

	public static void main(String[] args) {
		try{new Inducer(args);}
		catch(IOException e){e.printStackTrace();}
	}
}
