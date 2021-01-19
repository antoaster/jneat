package jneat;

public interface FitnessFunction {
    double getMaxFitness();
    double[] computeFitness(int _sample, int _num_nodes, double _out[][], double _tgt[][]);
}