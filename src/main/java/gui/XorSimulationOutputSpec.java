package gui;

import jneat.SimulationOutputSpec;

public class XorSimulationOutputSpec implements SimulationOutputSpec {

    @Override
    public int getNumUnit() {
        return 1;
    }

    @Override
    public double getTarget(int _plist[]) {

        int _index = _plist[0];
        int _col = _plist[1];

        if (_index < 0)
            _index = -_index;

        if (_index >= 4)
            _index = _index % 4;

        double d[] = new double[4];

        d[0] = 0;
        d[1] = 1;
        d[2] = 1;
        d[3] = 0;

        return d[_index];

    }

} 
