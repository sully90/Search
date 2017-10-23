package engine;

import ml.neuralnet.Net;
import ml.neuralnet.models.Layer;
import ml.neuralnet.models.Learnable;
import ml.neuralnet.models.Neuron;
import ml.neuralnet.models.Topology;
import ml.nlp.stanford.StanfordNLPHelper;
import persistence.elastic.ElasticHelper;
import persistence.elastic.client.ElasticSearchClient;
import persistence.elastic.utils.ElasticIndex;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/*
This class brings together both the ElasticSearchClient and the coupling to our ANN
 */
public class SearchEngine<T> extends ElasticSearchClient {

    private StanfordNLPHelper nlpHelper;
    private Net myNet;  // The neural net associated with this SearchEngine.

    public SearchEngine(Topology topology, ElasticIndex elasticIndex, Class<T> returnClass) throws UnknownHostException {
        this(new Net(topology), elasticIndex, returnClass);
    }

    public List<Double> getLayerOutputs(int index) {
        final Layer layer = this.myNet.getLayer(index);

        List<Double> outputs = new LinkedList<>();
        for (int n = 0; n < layer.getSize() - 1; n++) {
            Neuron neuron = layer.get(n);
            outputs.add(neuron.getOutputVal());
        }

        return outputs;
    }

    public SearchEngine(Net myNet, ElasticIndex elasticIndex, Class<T> returnClass) throws UnknownHostException {
        super(ElasticHelper.getClient(ElasticHelper.Host.LOCALHOST), elasticIndex, returnClass);
        this.nlpHelper = new StanfordNLPHelper();
        this.myNet = myNet;
    }

    public double meanSentimentOfQuery(String queryText) throws Exception {
        return this.nlpHelper.getMeanSentiment(queryText);
    }

    public List<Double> feedForwardAndSort(List<Learnable> learnableList) throws Exception {
        return this.feedForwardAndSort(learnableList, null);
    }

    public List<Double> feedForwardAndSort(List<Learnable> learnableList, List<List<Double>> additionalInputVals) throws Exception {
        // Feed-forward to get sort order

        Learnable learnable;

        List<Double> inputVals;
        List<Double> resultVals;

        List<Double> scores = new LinkedList<>();

        // Make sure we only have 1 output neuron
        Topology topology = this.myNet.getTopology();
        if (topology.get(topology.getSize() - 1) != 1) {
            throw new Exception(String.format("Got unexpected number of output neurons: %d", topology.get(topology.getSize() - 1)));
        }

        // Retrain the model based on the features
        for (int m = 0; m < learnableList.size() - 1; m++) {
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

            this.myNet.feedForward(inputVals);

            // Add the output to the scores list
            resultVals = this.myNet.getResults();
            scores.add(resultVals.get(0));
            System.out.println(inputVals + " : " + resultVals.get(0));
        }

        return scores;
    }

    public void updateNeuralNet(List<Learnable> learnableList) {
        this.updateNeuralNet(learnableList, 1);
    }

    public void updateNeuralNet(List<Learnable> learnableList, int nIterations) {
        this.updateNeuralNet(learnableList, null, nIterations);
    }

    public void updateNeuralNet(List<Learnable> learnableList, List<List<Double>> additionalInputVals) {
        this.updateNeuralNet(learnableList, additionalInputVals, 1);
    }

    public void updateNeuralNet(List<Learnable> learnableList,
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
                this.myNet.executeTrainingStep(inputVals, targetVal);
            }
        }
    }

}
