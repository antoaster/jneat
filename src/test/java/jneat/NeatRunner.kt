package jneat

import gui.Session
import jNeatCommon.EnvConstant
import jNeatCommon.EnvRoutine
import jNeatCommon.IOseq
import log.HistoryLog
import java.util.*

class NeatRunner(// Input / config
        private val fitnessFunction: FitnessFunction, private val inputSpec: SimulationInputSpec, private val outputSpec: SimulationOutputSpec) {
    protected var logger: HistoryLog = StdOutLog()

    // epoch state:
    var v1_fitness: MutableList<Double> = ArrayList()
    var v1_fitness_win: MutableList<Double> = ArrayList()
    var v1_species: MutableList<Double> = ArrayList()
    fun startNeat(): Boolean {
        var rc: Boolean
        var curr_name_pop_specie: String?
        var u_pop: Population? = null
        var u_genome: Genome? = null
        val xline: String
        val xFile: IOseq
        val id: Int
        var expcount: Int
        var u_prb_link = 0.5
        var u_recurrent = false
        var u_max_unit = 0
        var u_inp_unit = 0
        var u_out_unit = 0
        var gen: Int


        // imposta classe dinamica per fitness
        //
        try {
            EnvConstant.NR_UNIT_INPUT = inputSpec.numUnit
            EnvConstant.NUMBER_OF_SAMPLES = inputSpec.numSamples
            EnvConstant.NR_UNIT_OUTPUT = outputSpec.numUnit
            logger.sendToLog(" generation:  real    okay setting size inp ->" + EnvConstant.NR_UNIT_INPUT)
            logger.sendToLog(" generation:  real    okay setting size out ->" + EnvConstant.NR_UNIT_OUTPUT)
            logger.sendToLog(" generation:  real    okay setting sample   ->" + EnvConstant.NUMBER_OF_SAMPLES)
        } catch (ed: Exception) {
            ed.printStackTrace()
            logger.sendToLog(" generation: error in startNeat() $ed")
        }
        try {
            logger.sendToLog(" generation:      read parameter file of neat ...")
            Neat.initbase()
            rc = Neat.readParam(EnvRoutine.getJneatParameter())
            if (!rc) {
                logger.sendToLog(" generation: error in read " + EnvRoutine.getJneatParameter())
                return false
            }
            logger.sendToLog(" generation:   ok! " + EnvRoutine.getJneatParameter())
            //
            // gestisce start da genoma unico
            //
            if (EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_GENOME && !EnvConstant.FORCE_RESTART) {
                xFile = IOseq(EnvRoutine.getJneatFileData(EnvConstant.NAME_GENOMEA))
                rc = xFile.IOseqOpenR()
                if (!rc) {
                    logger.sendToLog(" generation:   error open " + EnvRoutine.getJneatFileData(EnvConstant.NAME_GENOMEA))
                    return false
                }
                logger.sendToLog(" generation:      open file genome " + EnvRoutine.getJneatFileData(EnvConstant.NAME_GENOMEA) + "...")
                xline = xFile.IOseqRead()
                val st = StringTokenizer(xline)
                //skip something?
                var curword = st.nextToken()
                //id of genome can be readed
                curword = st.nextToken()
                id = curword.toInt()
                logger.sendToLog(" generation:  ok! created genome id -> $id")
                u_genome = Genome(id, xFile)
            }

            //
            // gestisce start da popolazione random
            //
            if (EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_NEW_RANDOM_POPULATION && !EnvConstant.FORCE_RESTART) {
                logger.sendToLog(" generation:      cold start from random population.. ")
                u_prb_link = EnvConstant.PROBABILITY_OF_CONNECTION
                u_recurrent = EnvConstant.RECURSION
                u_max_unit = EnvConstant.NR_UNIT_MAX
                u_inp_unit = EnvConstant.NR_UNIT_INPUT
                u_out_unit = EnvConstant.NR_UNIT_OUTPUT
            }


            //
            // gestisce start da popolazione esistente
            //
            if (EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_OLD_POPULATION || EnvConstant.FORCE_RESTART) {
                logger.sendToLog(" generation:      warm start from old population -> " + EnvRoutine.getJneatFileData(EnvConstant.NAME_CURR_POPULATION))
            }


            //			EnvConstant.SERIAL_WINNER = 0;
            EnvConstant.SERIAL_SUPER_WINNER = 0
            EnvConstant.MAX_WINNER_FITNESS = 0.0

            // 1.6.2002 : reset pointer to first champion
            EnvConstant.CURR_ORGANISM_CHAMPION = null
            EnvConstant.FIRST_ORGANISM_WINNER = null
            EnvConstant.RUNNING = true
            expcount = 0
            while (expcount < Neat.p_num_runs) {
                logger.sendToLog(" generation:      Spawned population ...")
                if (EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_GENOME && !EnvConstant.FORCE_RESTART) u_pop = Population(u_genome!!, Neat.p_pop_size)
                if (EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_NEW_RANDOM_POPULATION && !EnvConstant.FORCE_RESTART) u_pop = Population(Neat.p_pop_size, u_inp_unit + 1, u_out_unit, u_max_unit, u_recurrent, u_prb_link)
                if (EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_OLD_POPULATION || EnvConstant.FORCE_RESTART) u_pop = Population(EnvRoutine.getJneatFileData(EnvConstant.NAME_CURR_POPULATION))
                logger.sendToLog(" generation:      Verifying Spawned Pop....")
                u_pop!!.verify()

                // start ............
                //
                gen = 1
                while (gen <= EnvConstant.NUMBER_OF_EPOCH) {

                    //		        curr_name_pop_specie =  EnvConstant.PREFIX_SPECIES_FILE + fmt5.format(gen);
                    curr_name_pop_specie = EnvConstant.PREFIX_SPECIES_FILE
                    //
                    EnvConstant.SUPER_WINNER_ = false
                    val success = epoch(u_pop, gen, curr_name_pop_specie)
                    logger.sendToStatus(" running generation ->$gen")
                    if (EnvConstant.STOP_EPOCH) break
                    gen++
                }
                if (EnvConstant.STOP_EPOCH) break
                expcount++
            }


            // before exit save last population
            u_pop!!.print_to_file_by_species(EnvRoutine.getJneatTempFile(EnvConstant.NAME_CURR_POPULATION))
            logger.sendToLog(" generation:      saved curr pop file " + EnvRoutine.getJneatTempFile(EnvConstant.NAME_CURR_POPULATION))
            logger.sendToLog(" generation:  READY for other request")
        } catch (e1: Throwable) {
            e1.printStackTrace()
            logger.sendToLog(" error in generation.startNeat() $e1")
        }
        EnvConstant.RUNNING = false
        logger.sendToStatus("READY")
        return true
    }

    fun epoch(
            pop: Population?,
            generation: Int,
            filename: String?): Boolean {
        val winner_prefix = EnvConstant.PREFIX_WINNER_FILE
        var esito: Boolean
        var winners = 0
        if (generation == 1) {
            v1_fitness_win = ArrayList()
            v1_fitness = ArrayList()
            v1_species = ArrayList()
        }
        return try {

            // Evaluate each organism if exist the winner.........
            // flag and store only the first winner
            var itr_organism: Iterator<Organism?>
            itr_organism = pop!!.organisms.iterator()
            var max_fitness_of_winner = 0.0
            while (itr_organism.hasNext()) {
                //point to organism
                val _organism: Organism = itr_organism.next()

                //evaluate
                esito = evaluate(_organism)

                // if is a winner , store a flag
                if (esito) {
                    winners++
                    if (_organism.fitness > max_fitness_of_winner) {
                        max_fitness_of_winner = _organism.fitness
                        EnvConstant.MAX_WINNER_FITNESS = max_fitness_of_winner
                    }
                    //
                    // 01.06.2002 : store only first organism
                    if (EnvConstant.FIRST_ORGANISM_WINNER == null) {
                        EnvConstant.FIRST_ORGANISM_WINNER = _organism
                        // System.out.print("\n okay flagged first *****");
                    }
                }
            }

            //compute average and max fitness for each species
            val itr_specie: Iterator<Species>
            itr_specie = pop.species.iterator()
            while (itr_specie.hasNext()) {
                val _specie = itr_specie.next()
                _specie.compute_average_fitness()
                _specie.compute_max_fitness()
            }
            // Only print to file every print_every generations
            var cause1 = " "
            var cause2 = " "
            if (generation % Neat.p_print_every == 0 || winners > 0) {
                if (generation % Neat.p_print_every == 0) cause1 = " request"
                if (winners > 0) cause2 = " winner"
                val name_of_specie = EnvRoutine.getJneatTempFile(filename) + generation
                pop.print_to_file_by_species(name_of_specie)
                logger.sendToLog(" generation:      write/rewrite file specie  $name_of_specie -> $cause1$cause2")
            }

            // if exist a winner write to file
            if (winners > 0) {
                var name_of_winner: String
                logger.sendToLog(" generation:      in this generation $generation i have found at leat one WINNER  ")
                var conta = 0
                itr_organism = pop.organisms.iterator()
                while (itr_organism.hasNext()) {
                    val _organism: Organism = itr_organism.next()
                    if (_organism.winner) {
                        name_of_winner = EnvRoutine.getJneatTempFile(winner_prefix) + generation + "_" + _organism.genome.genome_id
                        _organism.genome.print_to_filename(name_of_winner)
                        // EnvConstant.SERIAL_WINNER++;
                        conta++
                    }
                    if (EnvConstant.SUPER_WINNER_) {
                        logger.sendToLog(" generation:      in this generation $generation i have found a SUPER WINNER ")
                        name_of_winner = EnvRoutine.getJneatTempFile(winner_prefix) + "_SUPER_" + generation + "_" + _organism.genome.genome_id
                        _organism.genome.print_to_filename(name_of_winner)
                        //  EnvConstant.SERIAL_SUPER_WINNER++;
                        EnvConstant.SUPER_WINNER_ = false
                    }
                }
                logger.sendToLog(" generation:      number of winner's is $conta")
            }

            // wait an epoch and make a reproduction of the best species
            //
            pop.epoch(generation)
            if (!EnvConstant.REPORT_SPECIES_TESTA.equals("", ignoreCase = true)) {
                if (winners > 0) logger.sendToLog(" generation:    This specie contains the  << WINNER >> ")
            }
            v1_species.add(pop.species.size.toDouble())
            v1_fitness.add(pop.highest_fitness)
            v1_fitness_win.add(EnvConstant.MAX_WINNER_FITNESS)
            winners > 0
        } catch (e: Exception) {
            e.printStackTrace()
            print("\n exception in generation.epoch ->$e")
            System.exit(12)
            false
        }
    }

    fun evaluate(organism: Organism): Boolean {
        var fit_dyn = 0.0
        var err_dyn = 0.0
        var win_dyn = 0.0

        // per evitare errori il numero di ingressi e uscite viene calcolato in base
        // ai dati ;
        // per le unit di input a tale numero viene aggiunto una unit bias
        // di tipo neuron
        // le classi di copdifica input e output quindi dovranno fornire due
        // metodi : uno per restituire l'input j-esimo e uno per restituire
        // il numero di ingressi/uscite
        // se I/O Ã¨ da file allora Ã¨ il metodo di acesso ai files che avrÃ  lo
        // stesso nome e che farÃ  la stessa cosa.
        val _net: Network
        var success = false
        var errorsum = 0.0
        var net_depth = 0 //The max depth of the network to be activated
        var count = 0
        val `in` = DoubleArray(EnvConstant.NR_UNIT_INPUT + 1)

        // setting bias
        `in`[EnvConstant.NR_UNIT_INPUT] = 1.0
        val out = Array<DoubleArray?>(EnvConstant.NUMBER_OF_SAMPLES) { DoubleArray(EnvConstant.NR_UNIT_OUTPUT) }
        val tgt = Array<DoubleArray?>(EnvConstant.NUMBER_OF_SAMPLES) { DoubleArray(EnvConstant.NR_UNIT_OUTPUT) }
        val ns = EnvConstant.NUMBER_OF_SAMPLES
        _net = organism.net
        net_depth = _net.max_depth()

        // pass the number of node in genome for add a new
        // parameter of evaluate the fitness
        //
        val nn = _net.allnodes.size
        try {
            val plist_in = IntArray(2)
            count = 0
            while (count < EnvConstant.NUMBER_OF_SAMPLES) {
                plist_in[0] = count
                // first activation from sensor to first next level of neurons
                for (j in 0 until EnvConstant.NR_UNIT_INPUT) {
                    plist_in[1] = j
                    val v1 = inputSpec.getInput(plist_in)
                    `in`[j] = v1
                }


                // load sensor
                _net.load_sensors(`in`)

                // Maybe need one more?
                success = (0..net_depth).map {
                    _net.activate()
                }.last()

                // for each sample save each output
                for (j in 0 until EnvConstant.NR_UNIT_OUTPUT) out[count]!![j] = _net.outputs[j].activation

                // clear net
                _net.flush()
                count++
            }
        } catch (e2: Exception) {
            print("\n Error generic in Generation.input signal : err-code = \n$e2")
            print("\n re-run this application when the class is ready\n\t\t thank! ")
            System.exit(8)
        }


        // control the result
        if (success) {
            try {


                // prima di passare a calcolare il fitness legge il tgt da ripassare
                // al chiamante;
                val plist_tgt = IntArray(2)
                count = 0
                while (count < EnvConstant.NUMBER_OF_SAMPLES) {

                    //					 System.out.print("\n sample : "+count);
                    plist_tgt[0] = count
                    for (j in 0 until EnvConstant.NR_UNIT_OUTPUT) {
                        plist_tgt[1] = j
                        val v1 = outputSpec.getTarget(plist_tgt)
                        //						System.out.print(" ,  o["+j+"] = "+v1);
                        tgt[count]!![j] = v1
                    }
                    count++
                }
                val fitness = fitnessFunction.computeFitness(ns, nn, out, tgt)
                fit_dyn = fitness!![0]
                err_dyn = fitness[1]
                win_dyn = fitness[2]


                //			   System.out.print("\n ce so passo!");
            } catch (e3: Exception) {
                print("\n Error generic in Generation.success : err-code = \n$e3")
                print("\n re-run this application when the class is ready\n\t\t thank! ")
                e3.printStackTrace()
                System.exit(8)
            }
            organism.fitness = fit_dyn
            organism.error = err_dyn
        } else {
            errorsum = 999.0
            organism.fitness = 0.001
            organism.error = errorsum
        }
        if (win_dyn == 1.0) {
            organism.winner = true
            return true
        }
        if (win_dyn == 2.0) {
            organism.winner = true
            EnvConstant.SUPER_WINNER_ = true
            return true
        }
        organism.winner = false
        return false
    }

    fun doGlobalConfigStuff() {
        // "Load default" button
        loadParameters()

        // "Load sess default" button
        loadSession()

        // ??
        EnvRoutine.getSession()
    }

    private fun loadParameters() {
        logger.sendToLog(" loading file parameter...")
        Neat.initbase()
        val name = EnvRoutine.getJneatParameter()
        val rc = Neat.readParam(name)
        if (!rc) {
            throw RuntimeException("Bad config!: [$name]")
        }
        logger.sendToLog(" ok file parameter $name loaded!")
        logger.sendToStatus("READY")
    }

    private fun loadSession() {
        logger.sendToStatus("wait....")
        EnvConstant.EDIT_STATUS = 0 //TF?
        val nomef = EnvRoutine.getJneatSession()
        logger.sendToLog(" session: wait loading -> $nomef")
        var xline: String
        val xFile: IOseq
        xFile = IOseq(nomef)
        val rc = xFile.IOseqOpenR()
        if (rc) {
            val sb1 = StringBuilder()
            xline = xFile.IOseqRead()
            while (xline != "EOF") {
                sb1.append(xline).append("\n")
                xline = xFile.IOseqRead()
            }
            val source_new = convertToArray(sb1.toString())
            setSourceNew(source_new)
            logger.sendToLog(" ok file loaded")
            logger.sendToStatus("READY")
            xFile.IOseqCloseR()
        } else {
            throw RuntimeException("Bad session file [$nomef]")
        }
    }

    fun setSourceNew(_source: Array<String?>) {
        var riga: StringTokenizer
        var elem: String
        var sz: Int
        var prev_word: String?
        try {
            for (i in _source.indices) {
                // search name for fitness class;
                // search i/o class or files for input/target signal
                //
                val b1 = IntArray(_source[i]!!.length)
                for (j in b1.indices) b1[j] = 0
                val zriga = _source[i]
                var pos = 0
                for (k in Session.My_keyword.indices) {
                    val ckey = Session.My_keyword[k]
                    pos = zriga!!.indexOf(ckey, 0)
                    if (pos != -1) {
                        for (k1 in 0 until ckey.length) b1[pos + k1] = 1
                        var done = false
                        var offset = pos + ckey.length
                        while (!done) {
                            pos = zriga.indexOf(ckey, offset)
                            if (pos != -1) {
                                for (k1 in 0 until ckey.length) b1[pos + k1] = 1
                                offset = pos + ckey.length
                            } else done = true
                        }
                    }
                }
                var n1 = 0
                var v1 = 0
                var v2 = 0
                var k2 = 0
                var comment = false
                for (k1 in b1.indices) {
                    v1 = b1[k1]
                    if (v1 == 1) {
                        if (zriga!!.substring(k1, k1 + 1) == ";") {
                            comment = true
                            break
                        } else comment = false
                        break
                    }
                }
                if (!comment) {
                    // cerca fino a che non trova n1 != n2;
                    //int lun = 0;
                    var again = true
                    var k1 = 0
                    while (k1 < b1.size) {
                        v1 = b1[n1]
                        again = false
                        k2 = n1 + 1
                        while (k2 < b1.size) {
                            v2 = b1[k2]
                            if (v2 != v1) {
                                again = true
                                break
                            }
                            k2++
                        }


                        //System.out.print("\n n1="+n1+" n2="+n2+" found elem ->"+elem+"<- size("+elem.length()+")");
                        k1 = k2
                        n1 = k2
                        k1++
                    }
                    riga = StringTokenizer(zriga)
                    sz = riga.countTokens()
                    prev_word = null
                    for (r in 0 until sz) {
                        elem = riga.nextToken()
                        for (k in Session.My_keyword.indices) {
                            if (Session.My_keyword[k].equals(elem, ignoreCase = true)) {
                                break
                            }
                        }
                        if (prev_word != null && prev_word.equals("data_from_file", ignoreCase = true)) {
                            if (elem.equals("Y", ignoreCase = true)) {
                                EnvConstant.TYPE_OF_SIMULATION = EnvConstant.SIMULATION_FROM_FILE
                            }
                        }
                        if (prev_word != null && prev_word.equals("data_from_class", ignoreCase = true)) {
                            if (elem.equals("Y", ignoreCase = true)) {
                                EnvConstant.TYPE_OF_SIMULATION = EnvConstant.SIMULATION_FROM_CLASS
                            }
                        }
                        prev_word = elem
                    }
                }
            }
        } catch (e1: Exception) {
            logger.sendToStatus(" session: Couldn't insert initial text.:$e1")
        }
    }

    fun convertToArray(_text: String): Array<String?> {
        val riga: StringTokenizer
        var elem: String
        val sz: Int
        riga = StringTokenizer(_text, "\n")
        sz = riga.countTokens()
        val source_new = arrayOfNulls<String>(sz)
        for (r in 0 until sz) {
            elem = riga.nextToken()
            //   System.out.print("\n conv.to.string --> elem["+r+"] --> "+elem);
            source_new[r] = """
                $elem
                
                """.trimIndent()
        }
        return source_new
    }

    val lastGenerationFitness: List<Double>
        get() = v1_fitness
    val lastGenerationWinnersFitness: List<Double>
        get() = v1_fitness_win
    val lastGenerationSpeciesSomething: List<Double>
        get() = v1_species
}