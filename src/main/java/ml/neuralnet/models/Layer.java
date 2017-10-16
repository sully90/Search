package ml.neuralnet.models;

import java.util.LinkedList;
import java.util.List;

public class Layer {

    private List<Neuron> neurons;

    public Layer() {
        this.neurons = new LinkedList<>();
    }

    public void add(Neuron neuron) {
        this.neurons.add(neuron);
    }

    public List<Neuron> getNeurons() {
        return this.neurons;
    }

    public int getSize() {
        return this.neurons.size();
    }

    public Neuron get(int index) {
        return this.neurons.get(index);
    }

}
