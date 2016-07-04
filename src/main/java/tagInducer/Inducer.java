package tagInducer;

import tagInducer.corpus.CCGJSONCorpus;
import tagInducer.corpus.Corpus;
import tagInducer.features.*;
import tagInducer.utils.CollectionUtils;
import tagInducer.utils.FileUtils;
import tagInducer.utils.NotificationSender;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
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

		if (o.getCorpusFileName().contains("json"))
			corpus = new CCGJSONCorpus(o);
		else corpus = new Corpus(o);

		System.out.println("Corpus:\t" + corpus.getNumSentences() + " sents\t" +
				corpus.getNumTokens() + " tokens\t" + corpus.getNumTypes() + " types.");

		// A map of feature vectors per word type.
		// String will contain the feature type and
		// int[][] is a MxF array of features per type.
		Map<String, int[][]> featureVectors = new HashMap<>();

		if (featureTypes.contains(FeatureNames.CONTEXT)) {
			Features features = new ContextFeatures(corpus);
			featureVectors.put(FeatureNames.CONTEXT, features.getFeatures());
		}
		if (featureTypes.contains(FeatureNames.DEPS)) {
			Features features = new DepFeatures(corpus, o.isUndirDeps());
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
			String type = (o.getPargFeatType() == null) ? "all" : o.getPargFeatType();
            // Special case where the features are broken into 3 categories
            PargFeatures features = new PargFeatures(corpus);
            switch (type) {
				case "all":
					featureVectors.put(FeatureNames.PARG + ":cat", features.getCatFeatures());
					featureVectors.put(FeatureNames.PARG + ":headCat", features.getHeadCatFeatures());
					featureVectors.put(FeatureNames.PARG + ":context", features.getContextFeatures());
					break;
				case "cat":
					featureVectors.put(FeatureNames.PARG + ":cat", features.getCatFeatures());
					break;
				case "headcat":
					featureVectors.put(FeatureNames.PARG + ":headCat", features.getHeadCatFeatures());
					break;
				case "context":
					featureVectors.put(FeatureNames.PARG + ":context", features.getContextFeatures());
					break;
				default:
					System.err.println("Wrong PARG feature type: " + type + ". " +
							"Available types: cat, headcat, context, all");
					System.exit(-1);
			}
		}
		if (featureTypes.contains(FeatureNames.CCGCATS)) {
			Features features = new CCGCatFeatures(corpus);
			featureVectors.put(FeatureNames.CCGCATS, features.getFeatures());
		}

		for (String feat : featureVectors.keySet()) {
			System.out.println(feat + "\t" + featureVectors.get(feat)[0].length);
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

		corpus.setCorpusClusters(sampler.getFinalAssignment());

		String evalScores = null;
		// Run an evaluation at the end
		if (corpus.hasTags()) {
			System.out.println();
			Evaluator eval = new Evaluator(corpus);
			evalScores = eval.scoresSummary();
			System.out.println(evalScores);
		}

		// Output the final logLikelihood
		System.out.println("Final LL: " + sampler.getBestClassLogP());

		//Output the tagged file
		corpus.writeTagged(o.getOutFile());

        if (o.generateDistributions()) {
            //TODO Create a more sophisticated output file
            BufferedWriter out = FileUtils.createOut(o.getOutFile() + ".distr");
            double[][] array = sampler.getBestClassDistributions();
            for (int wordType = 0; wordType < array.length; wordType++) {
                double[] anArray = array[wordType];
                String distr = Arrays.toString(anArray);
                out.write(corpus.getWordString(wordType) + "\t" + distr.substring(1, distr.length()-1) + "\n");
            }
            out.close();
        }

		//Send a notification to android (if configured)
		if (o.getAPIKeyFile() != null) {
			String message = "Clustering took " + ((end-start) / 60000) + " minutes";
			if (evalScores != null)
				message += "\n" + evalScores;
			NotificationSender notificationSender = new NotificationSender(o.getAPIKeyFile());
			notificationSender.notify(message);
		}
		return (end - start);
	}

	public static void main(String[] args) {
		try{new Inducer(args);}
		catch(IOException e){e.printStackTrace();}
	}
}
