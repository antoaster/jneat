package jneat;

public interface SimulationInputSpec {
    int getNumSamples();

    int getNumUnit();

    double getInput(int _plist[]);
}
