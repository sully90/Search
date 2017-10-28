package com.sully90.elasticutils.ml.neuralnet.models;


import java.util.List;

public interface Learnable {

    List<Double> getInputVals();

    static double normalise(double val, double max, double min) {
        double normalised = (val - min) / (max - min);  // [0-1]
        return (normalised - 0.5d) * 2.0d;  // [-1, 1]
    }

}
