package com.sully90.elasticutils.ml.neuralnet.models;

import java.util.LinkedList;
import java.util.List;

public class Topology {
    private List<Integer> topology;

    public Topology(List<Integer> topology) {
        this.topology = topology;
    }

    private Topology() {
        // For Jackson
    }

    public Topology(int[] topology) {
        this.topology = new LinkedList<>();
        for (int i : topology) {
            this.topology.add(i);
        }
    }

    public Integer get(int index) {
        return this.topology.get(index);
    }

    public int getSize() {
        return this.topology.size();
    }
}
