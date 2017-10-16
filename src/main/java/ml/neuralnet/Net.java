package ml.neuralnet;

import ml.neuralnet.models.Layer;
import ml.neuralnet.models.Neuron;
import ml.neuralnet.models.Topology;
import org.bson.types.ObjectId;
import persistence.mongo.WritableObject;
import persistence.mongo.util.CollectionNames;
import persistence.mongo.util.ObjectFinder;
import persistence.mongo.util.ObjectWriter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Net implements WritableObject {

    private static double errorThresh = 0.2;

    private List<Layer> layers;
    private Topology topology;

    private ObjectId _id;

    private int numLayers;

    private double error;
    private double recentAverageError;
    private double recentAverageErrorSmoothingFactor = 100.0;

    public Net(Topology topology) {
        this.layers = new LinkedList<>();

        this.topology = topology;
        this.numLayers = this.topology.getSize();

        for(int layerNum = 0; layerNum < this.numLayers; layerNum++) {
            Layer layer = new Layer();

            int numOutputs = layerNum == topology.getSize() - 1 ? 0 : topology.get(layerNum + 1);

            // We have a new layer, now fill it with neurons, and add bias neuron
            for(int neuronNum = 0; neuronNum <= topology.get(layerNum); neuronNum++) {
                layer.add(new Neuron(numOutputs, neuronNum));
            }

            // Set bias (last) neurons output to 1.0
            layer.get(layer.getSize() - 1).setOutputVal(-1.0);
            layer.get(layer.getSize() - 1).flagBiasNeuron();
            this.layers.add(layer);
        }
    }

    private Net() {
        // For Jackson
    }

    private Layer getOutputLayer() {
        return this.layers.get(this.layers.size() - 1);
    }

    public void feedForward(final List<Double> inputVals) {
        assert(inputVals.size() == this.layers.get(0).getSize() - 1);  // - 1 to account for bias

        // Assign (latch) the input values into the input neurons
        for(int i = 0; i < inputVals.size(); i++) {
            this.layers.get(0).get(i).setOutputVal(inputVals.get(i));
        }

        // Forward propagate
        for(int layerNum = 1; layerNum < this.layers.size(); layerNum++) {  // skip input, start with first hidden layer
            Layer prevLayer = this.layers.get(layerNum - 1);
            for(int n = 0; n < this.layers.get(layerNum).getSize() - 1; n++) {
                this.layers.get(layerNum).get(n).feedForward(prevLayer);
            }
        }
    }

    public void backProp(final List<Double> targetVals) {
        // Calculate overall net error (RMS)

        Layer outputLayer = this.getOutputLayer();
        this.error = 0.0;

        for(int n = 0; n < outputLayer.getSize() - 1; n++) {
            double delta = targetVals.get(n) - outputLayer.get(n).getOutputVal();
            this.error += delta * delta;
        }
        this.error /= outputLayer.getSize() - 1;  // get average error squared
        this.error = Math.sqrt(this.error); // RMS

        // Implement a recent average measurement
        this.recentAverageError = (this.recentAverageError * this.recentAverageErrorSmoothingFactor + this.error)
                / (this.recentAverageErrorSmoothingFactor + 1.0);

        // Calculate output layer gradiends
        for(int n = 0; n < outputLayer.getSize() - 1; n++) {
            outputLayer.get(n).calcOutputGradients(targetVals.get(n));
        }

        // Calculate gradients on hidden layers
        for(int layerNum = this.layers.size() - 2; layerNum > 0; layerNum--) {  // start with the right most hidden layer
            Layer hiddenLayer = this.layers.get(layerNum);
            Layer nextLayer = this.layers.get(layerNum + 1);

            for(int n = 0; n < hiddenLayer.getSize(); n++) {
                hiddenLayer.get(n).calcHiddenGradients(nextLayer);
            }
        }

        // For all layers from outputs to first hidden layer,
        // update connection weights
        for(int layerNum = this.layers.size() - 1; layerNum > 0; layerNum--) {  // go through all layers, starting at right most and dont include the input layer (as theres no input weights)
            Layer layer = this.layers.get(layerNum);
            Layer prevLayer = this.layers.get(layerNum - 1);

            for(int n = 0; n < layer.getSize() - 1; n++) {
                layer.get(n).updateInputWeights(prevLayer);
            }
        }
    }

    public List<Double> getResults() {
        List<Double> resultVals = new LinkedList<>();

        Layer outputLayer = this.getOutputLayer();

        for(int n = 0; n < outputLayer.getSize() - 1; n++) {
            resultVals.add(outputLayer.get(n).getOutputVal());
        }
        return resultVals;
    }

    public Topology getTopology() {
        return topology;
    }

    public static void main(String[] args) {
        Topology topology = new Topology(new int[]{1, 5, 1});
        Net myNet = new Net(topology);

        Random random = new Random();

        List<Double> inputVals = new LinkedList<>();
        List<Double> targetVals = new LinkedList<>();

        for(int i = 0; i < 1000; i++) {
            double val = Math.PI * random.nextFloat();
            inputVals.add(val);
            targetVals.add(Math.sin(val));
        }

        int trainingPass = 0;
        for(int i = 0; i < inputVals.size(); i++) {
            trainingPass++;

            System.out.println(String.format("Training pass: %d", trainingPass));

            List<Double> inputVal = new LinkedList<>(Arrays.asList(inputVals.get(i)));
            List<Double> targetVal = new LinkedList<>(Arrays.asList(targetVals.get(i)));

            // feed forwards
            myNet.feedForward(inputVal);

            // Get the results
            List<Double> results = myNet.getResults();

            System.out.println("Results:");
            for(Double res : results) {
                System.out.println(res);
            }
            System.out.println("Error = " + myNet.error);

            // back-prop
            myNet.backProp(targetVal);
        }

//        myNet.writer().save();
        System.out.println("Done!");
    }

    @Override
    public ObjectWriter writer() {
        return new ObjectWriter(CollectionNames.NET, this);
    }

    @Override
    public ObjectId getObjectId() {
        return this._id;
    }

    public static ObjectFinder<Net> finder() {
        return new ObjectFinder<>(CollectionNames.NET, Net.class);
    }

    public static double getErrorThresh() {
        return errorThresh;
    }

    public static void setErrorThresh(double errorThresh) {
        Net.errorThresh = errorThresh;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public void setLayers(List<Layer> layers) {
        this.layers = layers;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    public int getNumLayers() {
        return numLayers;
    }

    public void setNumLayers(int numLayers) {
        this.numLayers = numLayers;
    }

    public double getError() {
        return error;
    }

    public void setError(double error) {
        this.error = error;
    }

    public double getRecentAverageError() {
        return recentAverageError;
    }

    public void setRecentAverageError(double recentAverageError) {
        this.recentAverageError = recentAverageError;
    }

    public double getRecentAverageErrorSmoothingFactor() {
        return recentAverageErrorSmoothingFactor;
    }

    public void setRecentAverageErrorSmoothingFactor(double recentAverageErrorSmoothingFactor) {
        this.recentAverageErrorSmoothingFactor = recentAverageErrorSmoothingFactor;
    }
}
