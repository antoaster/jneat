package jneat

interface FitnessFunction {
    val maxFitness: Double
    fun computeFitness(_sample: Int, _num_nodes: Int, _out: Array<DoubleArray?>?, _tgt: Array<DoubleArray?>?): DoubleArray?
}