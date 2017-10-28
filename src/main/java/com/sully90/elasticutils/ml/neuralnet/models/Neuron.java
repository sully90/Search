package com.sully90.elasticutils.ml.neuralnet.models;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Neuron {

    public static double eta = 0.15;
    public static double alpha = 0.25;

    private static Random random = new Random();

    private final int myIndex;

    private double outputVal;
    private double gradient;

    private boolean isBias = false;

    private List<Connection> outputWeights;

    public Neuron(int numOutputs, int myIndex) {
        outputWeights = new LinkedList<>();

        Connection connection;

        for(int i = 0; i < numOutputs; i++) {
            connection = new Connection();
            connection.weight = random.nextFloat();
            outputWeights.add(connection);
        }
        this.myIndex = myIndex;
    }

    private Neuron() {
        // For Jackson
        this.myIndex = 0;
    }

    public double getOutputVal() {
        return this.outputVal;
    }

    public void setOutputVal(double outputVal) {
        this.outputVal = outputVal;
    }

    public void flagBiasNeuron() {
        this.isBias = true;
    }

    public boolean isBias() {
        return this.isBias;
    }

    private static double transferFunction(double x) {
        return Math.tanh(x);
    }

    private static double transferFunctionDerivative(double x) {
        // tanh derivative
        return 1.0 - x * x;
    }

    private final double sumDOW(final Layer nextLayer) {
        double sum = 0.0;

        // Sum our contributions of the errors at the nodes we feed

        for(int n = 0; n < nextLayer.getSize() - 1; n++) {
            sum += this.outputWeights.get(n).weight * nextLayer.get(n).gradient;
        }

        return sum;
    }

    public void feedForward(final Layer prevLayer) {
        double sum = 0.0;

        // Sum the previous layer's outputs (which are our inputs)
        // Include the bias node from the previous layer

        for (int n = 0; n < prevLayer.getSize(); n++) {
            sum += prevLayer.get(n).getOutputVal() * prevLayer.get(n).outputWeights.get(this.myIndex).weight;
        }

        // If using a function whos derivative is not a function of itself (i.e not dtanh/dx = 1 - tanh(x)^2)
        // then just set m_outputVal to sum and implement the transfer function calculated in the getter
        this.outputVal = Neuron.transferFunction(sum);
    }

    public void calcOutputGradients(double targetVal) {
        double delta = targetVal - this.outputVal;
        this.gradient = delta * Neuron.transferFunctionDerivative(this.outputVal);
    }

    public void calcHiddenGradients(final Layer nextLayer) {
        double dow = this.sumDOW(nextLayer);
        this.gradient = dow * Neuron.transferFunctionDerivative(this.outputVal);
    }

    public void updateInputWeights(Layer prevLayer) {
        // The weights to be updated are in the Connection container
        // in the neurons in the preceding layer

        for(int n = 0; n < prevLayer.getSize(); n++) {
            Neuron neuron = prevLayer.get(n);
            double oldDeltaWeight = neuron.outputWeights.get(this.myIndex).deltaWeight;

            // Individual input, magnified by the gradient and train rate:
            double newDeltaWeight = Neuron.eta * neuron.getOutputVal() * this.gradient
                // Also add momentum = a fraction of the prev. delta weight
                + Neuron.alpha * oldDeltaWeight;

            neuron.outputWeights.get(this.myIndex).deltaWeight = newDeltaWeight;
            neuron.outputWeights.get(this.myIndex).weight += newDeltaWeight;
        }
    }

    public final int getMyIndex() {
        return myIndex;
    }

    public double getGradient() {
        return gradient;
    }

    public List<Connection> getOutputWeights() {
        return outputWeights;
    }
}
