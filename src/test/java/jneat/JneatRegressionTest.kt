package jneat

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import gui.SomeKindaSimulationInputSpec
import gui.XorFitnessFunction
import gui.XorSimulationOutputSpec
import org.junit.Test

class JneatRegressionTest {
    @Test
    fun lifeUhFindsAWay() {
        val neatRunner = NeatRunner(
                XorFitnessFunction(),
                SomeKindaSimulationInputSpec(),
                XorSimulationOutputSpec())

        neatRunner.doGlobalConfigStuff()
        neatRunner.startNeat()

        val lastGenerationFitness = neatRunner.lastGenerationFitness
        val lastGenerationWinnersFitness = neatRunner.lastGenerationWinnersFitness
        val lastGenerationSpeciesSomething = neatRunner.lastGenerationSpeciesSomething

        val maxFitness: Double = lastGenerationWinnersFitness.maxOrNull()!!
        assertThat(maxFitness, greaterThan(10.0))

        //TODO: Assert more things. Extract a thing that can solve XOR?
    }
}