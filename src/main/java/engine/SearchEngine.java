package engine;

import ml.neuralnet.Net;
import ml.neuralnet.models.Learnable;
import ml.nlp.stanford.StanfordNLPHelper;
import persistence.elastic.ElasticHelper;
import persistence.elastic.client.ElasticSearchClient;
import persistence.elastic.utils.ElasticIndices;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/*
This class brings together both the ElasticSearchClient and the coupling to our ANN
 */
public class SearchEngine<T> extends ElasticSearchClient {

    private StanfordNLPHelper nlpHelper;

    public SearchEngine(ElasticIndices elasticIndex, Class<T> returnClass) throws UnknownHostException {
        super(ElasticHelper.getClient(ElasticHelper.Host.LOCALHOST), elasticIndex, returnClass);
        this.nlpHelper = new StanfordNLPHelper();
    }

    public double meanSentimentOfQuery(String queryText) throws Exception {
        return this.nlpHelper.getMeanSentiment(queryText);
    }

    public void updateNeuralNet(Net myNet, List<Learnable> learnableList) {
        this.updateNeuralNet(myNet, learnableList, 1);
    }

    public void updateNeuralNet(Net myNet, List<Learnable> learnableList, int nIterations) {
        this.updateNeuralNet(myNet, learnableList, null, nIterations);
    }

    public void updateNeuralNet(Net myNet, List<Learnable> learnableList, List<List<Double>> additionalInputVals) {
        this.updateNeuralNet(myNet, learnableList, additionalInputVals, 1);
    }

    public void updateNeuralNet(Net myNet, List<Learnable> learnableList,
                                List<List<Double>> additionalInputVals, int nIterations) {
        // Performs a training step using the list of learnables provided,
        // assuming they have been sorted to their new order of relevance.

        int nhits = learnableList.size();
        Learnable learnable;

        List<Double> inputVals;
        List<Double> targetVal;

        // Retrain the model based on the features
        for (int i = 0; i < nIterations; i++) {
            for (int m = 0; m < learnableList.size() - 1; m++) {
                // m is the new rank
                double newNormalisedRank = Learnable.normalise(m, 0, nhits - 1);
                // This is our targetVal
                // Perform a training step

                // Input vals
                learnable = learnableList.get(m);
                List<Double> mAdditionalInputVals = additionalInputVals.get(m);

                inputVals = learnable.getInputVals();
                // Add elasticsearch internal score to inputVals
                if (additionalInputVals != null) {
                    for (Double val : mAdditionalInputVals) {
                        inputVals.add(val);
                    }
                }

                // Target val is the normalised rank
                targetVal = new LinkedList<>(Arrays.asList(Double.valueOf(newNormalisedRank)));

                // Do the training step. We don't care about the results here.
                myNet.executeTrainingStep(inputVals, targetVal);
            }
        }
    }

}
