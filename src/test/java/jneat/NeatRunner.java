package jneat;

import jNeatCommon.EnvConstant;
import jNeatCommon.EnvRoutine;
import jNeatCommon.IOseq;
import log.HistoryLog;

import javax.swing.*;
import javax.swing.text.Document;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import static gui.Session.My_keyword;

public class NeatRunner {

    protected HistoryLog logger = new StdOutLog();

    // Input / config
    private FitnessFunction fitnessFunction;
    private SimulationInputSpec inputSpec;
    private SimulationOutputSpec outputSpec;


    // epoch state:
    List<Double> v1_fitness = new ArrayList<>();
    List<Double> v1_fitness_win = new ArrayList<>();
    List<Double> v1_species = new ArrayList<>();

    public NeatRunner(FitnessFunction fitnessFunction, SimulationInputSpec inputSpec, SimulationOutputSpec outputSpec) {
        this.fitnessFunction = fitnessFunction;
        this.inputSpec = inputSpec;
        this.outputSpec = outputSpec;
    }


    public boolean startNeat() {
        boolean rc;
        String curr_name_pop_specie;

        String mask5 = "00000";

        Population u_pop = null;
        Genome u_genome = null;
        StringTokenizer st;
        String curword;
        String xline;
        IOseq xFile;
        int id;
        int expcount;
        double u_prb_link = 0.5;
        boolean u_recurrent = false;
        int u_max_unit = 0;
        int u_inp_unit = 0;
        int u_out_unit = 0;
        int gen;


        // imposta classe dinamica per fitness
        //
        try {

            EnvConstant.MAX_FITNESS = fitnessFunction.getMaxFitness();

            EnvConstant.NR_UNIT_INPUT = inputSpec.getNumUnit();
            EnvConstant.NUMBER_OF_SAMPLES = inputSpec.getNumSamples();
            EnvConstant.NR_UNIT_OUTPUT = outputSpec.getNumUnit();

            logger.sendToLog(" generation:  real    okay setting size inp ->" + EnvConstant.NR_UNIT_INPUT);
            logger.sendToLog(" generation:  real    okay setting size out ->" + EnvConstant.NR_UNIT_OUTPUT);
            logger.sendToLog(" generation:  real    okay setting sample   ->" + EnvConstant.NUMBER_OF_SAMPLES);


        } catch (Exception ed) {
            ed.printStackTrace();
            logger.sendToLog(" generation: error in startNeat() " + ed);
        }


        try {


            logger.sendToLog(" generation:      read parameter file of neat ...");

            Neat u_neat = new Neat();
            u_neat.initbase();
            rc = u_neat.readParam(EnvRoutine.getJneatParameter());

            if (!rc) {
                logger.sendToLog(" generation: error in read " + EnvRoutine.getJneatParameter());
                return false;
            }


            logger.sendToLog(" generation:   ok! " + EnvRoutine.getJneatParameter());
            //
            // gestisce start da genoma unico
            //
            if ((EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_GENOME) && (!EnvConstant.FORCE_RESTART)) {

                xFile = new IOseq(EnvRoutine.getJneatFileData(EnvConstant.NAME_GENOMEA));
                rc = xFile.IOseqOpenR();
                if (!rc) {
                    logger.sendToLog(" generation:   error open " + EnvRoutine.getJneatFileData(EnvConstant.NAME_GENOMEA));
                    return false;
                }

                logger.sendToLog(" generation:      open file genome " + EnvRoutine.getJneatFileData(EnvConstant.NAME_GENOMEA) + "...");
                xline = xFile.IOseqRead();
                st = new StringTokenizer(xline);
                //skip
                curword = st.nextToken();
                //id of genome can be readed
                curword = st.nextToken();
                id = Integer.parseInt(curword);
                logger.sendToLog(" generation:  ok! created genome id -> " + id);
                u_genome = new Genome(id, xFile);


            }

            //
            // gestisce start da popolazione random
            //
            if (EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_NEW_RANDOM_POPULATION && (!EnvConstant.FORCE_RESTART)) {
                logger.sendToLog(" generation:      cold start from random population.. ");
                u_prb_link = EnvConstant.PROBABILITY_OF_CONNECTION;
                u_recurrent = EnvConstant.RECURSION;
                u_max_unit = EnvConstant.NR_UNIT_MAX;
                u_inp_unit = EnvConstant.NR_UNIT_INPUT;
                u_out_unit = EnvConstant.NR_UNIT_OUTPUT;
            }


            //
            // gestisce start da popolazione esistente
            //
            if ((EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_OLD_POPULATION) || (EnvConstant.FORCE_RESTART)) {
                logger.sendToLog(" generation:      warm start from old population -> " + EnvRoutine.getJneatFileData(EnvConstant.NAME_CURR_POPULATION));
            }


            //			EnvConstant.SERIAL_WINNER = 0;
            EnvConstant.SERIAL_SUPER_WINNER = 0;
            EnvConstant.MAX_WINNER_FITNESS = 0;

            // 1.6.2002 : reset pointer to first champion
            EnvConstant.CURR_ORGANISM_CHAMPION = null;
            EnvConstant.FIRST_ORGANISM_WINNER = null;

            EnvConstant.RUNNING = true;


            for (expcount = 0; expcount < u_neat.p_num_runs; expcount++) {

                logger.sendToLog(" generation:      Spawned population ...");


                if ((EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_GENOME) && (!EnvConstant.FORCE_RESTART))
                    u_pop = new Population(u_genome, u_neat.p_pop_size);

                if ((EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_NEW_RANDOM_POPULATION) && (!EnvConstant.FORCE_RESTART))
                    u_pop = new Population(u_neat.p_pop_size, (u_inp_unit + 1), u_out_unit, u_max_unit, u_recurrent, u_prb_link);

                if ((EnvConstant.TYPE_OF_START == EnvConstant.START_FROM_OLD_POPULATION) || (EnvConstant.FORCE_RESTART))
                    u_pop = new Population(EnvRoutine.getJneatFileData(EnvConstant.NAME_CURR_POPULATION));


                logger.sendToLog(" generation:      Verifying Spawned Pop....");
                u_pop.verify();

                // start ............
                //
                for (gen = 1; gen <= EnvConstant.NUMBER_OF_EPOCH; gen++) {
                    //		        curr_name_pop_specie =  EnvConstant.PREFIX_SPECIES_FILE + fmt5.format(gen);
                    curr_name_pop_specie = EnvConstant.PREFIX_SPECIES_FILE;
                    //
                    EnvConstant.SUPER_WINNER_ = false;
                    boolean esito = epoch(u_neat, u_pop, gen, curr_name_pop_specie);
                    logger.sendToStatus(" running generation ->" + gen);

                    if (EnvConstant.STOP_EPOCH)
                        break;

                }

                if (EnvConstant.STOP_EPOCH)
                    break;

            }


            // before exit save last population
            u_pop.print_to_file_by_species(EnvRoutine.getJneatFileData(EnvConstant.NAME_CURR_POPULATION));
            logger.sendToLog(" generation:      saved curr pop file " + EnvRoutine.getJneatFileData(EnvConstant.NAME_CURR_POPULATION));
            logger.sendToLog(" generation:  READY for other request");

        } catch (Throwable e1) {
            e1.printStackTrace();
            logger.sendToLog(" error in generation.startNeat() " + e1);
        }


        EnvConstant.RUNNING = false;

        logger.sendToStatus("READY");
        return true;


    }

    public boolean epoch(
            Neat _neat,
            Population pop,
            int generation,
            String filename) {

        String winner_prefix = EnvConstant.PREFIX_WINNER_FILE;
        String riga1 = null;
        boolean esito = false;
        int winners = 0;

        if (generation == 1) {
            v1_fitness_win = new ArrayList<>();
            v1_fitness = new ArrayList<>();
            v1_species = new ArrayList<>();
        }


        try {

            // Evaluate each organism if exist the winner.........
            // flag and store only the first winner

            Iterator<Organism> itr_organism;
            itr_organism = pop.organisms.iterator();
            double max_fitness_of_winner = 0.0;

            while (itr_organism.hasNext()) {
                //point to organism
                Organism _organism = ((Organism) itr_organism.next());

                //evaluate
                esito = evaluate(_organism);

                // if is a winner , store a flag
                if (esito) {
                    winners++;

                    if (_organism.getFitness() > max_fitness_of_winner) {
                        max_fitness_of_winner = _organism.getFitness();
                        EnvConstant.MAX_WINNER_FITNESS = max_fitness_of_winner;
                    }
                    //
                    // 01.06.2002 : store only first organism
                    if (EnvConstant.FIRST_ORGANISM_WINNER == null) {
                        EnvConstant.FIRST_ORGANISM_WINNER = _organism;
                        // System.out.print("\n okay flagged first *****");

                    }

                }
            }

            //compute average and max fitness for each species
            Iterator itr_specie;
            itr_specie = pop.species.iterator();
            while (itr_specie.hasNext()) {
                Species _specie = ((Species) itr_specie.next());
                _specie.compute_average_fitness();
                _specie.compute_max_fitness();
            }
            // Only print to file every print_every generations

            String cause1 = " ";
            String cause2 = " ";
            if (((generation % _neat.p_print_every) == 0) || (winners > 0)) {

                if ((generation % _neat.p_print_every) == 0)
                    cause1 = " request";
                if (winners > 0)
                    cause2 = " winner";

                String name_of_specie = EnvRoutine.getJneatTempFile(filename) + generation;
                pop.print_to_file_by_species(name_of_specie);
                logger.sendToLog(" generation:      write/rewrite file specie  " + name_of_specie + " -> " + cause1 + cause2);

            }

            // if exist a winner write to file
            if (winners > 0) {
                String name_of_winner;
                logger.sendToLog(" generation:      in this generation " + generation + " i have found at leat one WINNER  ");
                int conta = 0;
                itr_organism = pop.getOrganisms().iterator();
                while (itr_organism.hasNext()) {
                    Organism _organism = ((Organism) itr_organism.next());
                    if (_organism.winner) {
                        name_of_winner = EnvRoutine.getJneatTempFile(winner_prefix) + generation + "_" + _organism.getGenome().genome_id;
                        _organism.getGenome().print_to_filename(name_of_winner);
                        // EnvConstant.SERIAL_WINNER++;
                        conta++;
                    }
                    if (EnvConstant.SUPER_WINNER_) {
                        logger.sendToLog(" generation:      in this generation " + generation + " i have found a SUPER WINNER ");
                        name_of_winner = EnvRoutine.getJneatTempFile(winner_prefix) + "_SUPER_" + generation + "_" + _organism.getGenome().genome_id;
                        _organism.getGenome().print_to_filename(name_of_winner);
                        //  EnvConstant.SERIAL_SUPER_WINNER++;
                        EnvConstant.SUPER_WINNER_ = false;

                    }

                }

                logger.sendToLog(" generation:      number of winner's is " + conta);

            }

            // wait an epoch and make a reproduction of the best species
            //

            pop.epoch(generation);

            if (!EnvConstant.REPORT_SPECIES_TESTA.equalsIgnoreCase("")) {

                if (winners > 0)
                    logger.sendToLog(" generation:    This specie contains the  << WINNER >> ");

                if (!(EnvConstant.FIRST_ORGANISM_WINNER == null)) {
                    int idx = ((Organism) EnvConstant.FIRST_ORGANISM_WINNER).genome.genome_id;

                    if (winners > 0)
                        riga1 = "Time : " + generation + " genome (id=" + idx + ") is Current CHAMPION - WINNER ";
                    else
                        riga1 = "Time : " + generation + " genome (id=" + idx + ") is Current CHAMPION ";
                }
            }

            v1_species.add((double) pop.getSpecies().size());

            v1_fitness.add(pop.getHighest_fitness());

            v1_fitness_win.add(EnvConstant.MAX_WINNER_FITNESS);

            return winners > 0;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("\n exception in generation.epoch ->" + e);
            System.exit(12);
            return false;
        }

    }

    public boolean evaluate(Organism organism) {
        double fit_dyn = 0.0;
        double err_dyn = 0.0;
        double win_dyn = 0.0;

        // per evitare errori il numero di ingressi e uscite viene calcolato in base
        // ai dati ;
        // per le unit di input a tale numero viene aggiunto una unit bias
        // di tipo neuron
        // le classi di copdifica input e output quindi dovranno fornire due
        // metodi : uno per restituire l'input j-esimo e uno per restituire
        // il numero di ingressi/uscite
        // se I/O Ã¨ da file allora Ã¨ il metodo di acesso ai files che avrÃ  lo
        // stesso nome e che farÃ  la stessa cosa.

        Network _net = null;
        boolean success = false;

        double errorsum = 0.0;
        int net_depth = 0; //The max depth of the network to be activated
        int count = 0;


        //	  			   System.out.print("\n evaluate.step 1 ");

        double in[] = null;
        in = new double[EnvConstant.NR_UNIT_INPUT + 1];

        // setting bias

        in[EnvConstant.NR_UNIT_INPUT] = 1.0;

        double out[][] = null;
        out = new double[EnvConstant.NUMBER_OF_SAMPLES][EnvConstant.NR_UNIT_OUTPUT];

        double tgt[][] = null;
        tgt = new double[EnvConstant.NUMBER_OF_SAMPLES][EnvConstant.NR_UNIT_OUTPUT];

        Integer ns = new Integer(EnvConstant.NUMBER_OF_SAMPLES);


        _net = organism.net;
        net_depth = _net.max_depth();

        // pass the number of node in genome for add a new
        // parameter of evaluate the fitness
        //
        int xnn = _net.getAllnodes().size();
        Integer nn = new Integer(xnn);


        try {
            int[] plist_in = new int[2];

            for (count = 0; count < EnvConstant.NUMBER_OF_SAMPLES; count++) {
                plist_in[0] = count;
                // first activation from sensor to first next level of neurons
                for (int j = 0; j < EnvConstant.NR_UNIT_INPUT; j++) {
                    plist_in[1] = j;
                    double v1 = inputSpec.getInput(plist_in);
                    in[j] = v1;
                }


                // load sensor
                _net.load_sensors(in);
           /*
           // activate net
           success = _net.activate();

           // next activation while last level is reached !
           // use depth to ensure relaxation

           for (int relax = 0; relax <= net_depth; relax++)
            success = _net.activate();
           */

                if (EnvConstant.ACTIVATION_PERIOD == EnvConstant.MANUAL) {
                    for (int relax = 0; relax < EnvConstant.ACTIVATION_TIMES; relax++) {
                        success = _net.activate();
                    }
                } else {
                    // first activation from sensor to next layer....
                    success = _net.activate();

                    // next activation while last level is reached !
                    // use depth to ensure relaxation
                    for (int relax = 0; relax <= net_depth; relax++) {
                        success = _net.activate();
                    }
                }


                // for each sample save each output
                for (int j = 0; j < EnvConstant.NR_UNIT_OUTPUT; j++)
                    out[count][j] = ((NNode) _net.getOutputs().elementAt(j)).getActivation();

                // clear net
                _net.flush();
            }
        } catch (Exception e2) {
            System.out.print("\n Error generic in Generation.input signal : err-code = \n" + e2);
            System.out.print("\n re-run this application when the class is ready\n\t\t thank! ");
            System.exit(8);

        }


        // control the result
        if (success) {
            try {


                // prima di passare a calcolare il fitness legge il tgt da ripassare
                // al chiamante;
                int[] plist_tgt = new int[2];

                for (count = 0; count < EnvConstant.NUMBER_OF_SAMPLES; count++) {
                    //					 System.out.print("\n sample : "+count);

                    plist_tgt[0] = count;
                    for (int j = 0; j < EnvConstant.NR_UNIT_OUTPUT; j++) {
                        plist_tgt[1] = j;
                        double v1 = outputSpec.getTarget(plist_tgt);
                        //						System.out.print(" ,  o["+j+"] = "+v1);
                        tgt[count][j] = v1;
                    }
                }

                double[] fitness = fitnessFunction.computeFitness(ns, nn, out, tgt);

                fit_dyn = fitness[0];
                err_dyn = fitness[1];
                win_dyn = fitness[2];


                //			   System.out.print("\n ce so passo!");


            } catch (Exception e3) {
                System.out.print("\n Error generic in Generation.success : err-code = \n" + e3);
                System.out.print("\n re-run this application when the class is ready\n\t\t thank! ");
                e3.printStackTrace();
                System.exit(8);
            }

            organism.setFitness(fit_dyn);
            organism.setError(err_dyn);
        } else {
            errorsum = 999.0;
            organism.setFitness(0.001);
            organism.setError(errorsum);
        }


        if (win_dyn == 1.0) {
            organism.setWinner(true);
            return true;
        }

        if (win_dyn == 2.0) {
            organism.setWinner(true);
            EnvConstant.SUPER_WINNER_ = true;
            return true;
        }

        organism.setWinner(false);
        return false;


    }

    public void doGlobalConfigStuff() {
        // "Load default" button
        loadParameters();

        // "Load sess default" button
        loadSession();

        // ??
        EnvRoutine.getSession();
    }

    private void loadParameters() {
        logger.sendToLog(" loading file parameter...");
        Neat.initbase();
        String name = EnvRoutine.getJneatParameter();
        boolean rc = Neat.readParam(name);
        if (!rc) {
            throw new RuntimeException("Bad config!: [" + name + "]");
        }
        logger.sendToLog(" ok file parameter " + name + " loaded!");
        logger.sendToStatus("READY");
    }

    private void loadSession() {
        logger.sendToStatus("wait....");
        EnvConstant.EDIT_STATUS = 0; //TF?
        String nomef = EnvRoutine.getJneatSession();
        logger.sendToLog(" session: wait loading -> " + nomef);
        StringTokenizer st;
        String xline;
        IOseq xFile;

        xFile = new IOseq(nomef);
        boolean rc = xFile.IOseqOpenR();
        if (rc) {

            StringBuffer sb1 = new StringBuffer("");
            xline = xFile.IOseqRead();

            while (xline != "EOF") {
                sb1.append(xline + "\n");
                xline = xFile.IOseqRead();
            }

            String[] source_new = convertToArray(sb1.toString());
            setSourceNew(source_new);
            logger.sendToLog(" ok file loaded");
            logger.sendToStatus("READY");



            xFile.IOseqCloseR();

        } else {
            throw new RuntimeException("Bad session file [" + nomef + "]");
        }
    }

    public void setSourceNew(String[] _source) {
        StringTokenizer riga;
        String elem;
        int sz;
        String prev_word;
        boolean fnd;

        try {
            for (int i = 0; i < _source.length; i++) {
                // search name for fitness class;
                // search i/o class or files for input/target signal
                //

                int b1[] = new int[_source[i].length()];
                for (int j = 0; j < b1.length; j++)
                    b1[j] = 0;

                String zriga = _source[i];
                int pos = 0;

                for (int k = 0; k < My_keyword.length; k++) {
                    String ckey = My_keyword[k];
                    pos = zriga.indexOf(ckey, 0);
                    if (pos != -1) {
                        for (int k1 = 0; k1 < ckey.length(); k1++)
                            b1[pos + k1] = 1;
                        boolean done = false;
                        int offset = pos + ckey.length();
                        while (!done) {
                            pos = zriga.indexOf(ckey, offset);
                            if (pos != -1) {
                                for (int k1 = 0; k1 < ckey.length(); k1++)
                                    b1[pos + k1] = 1;
                                offset = pos + ckey.length();
                            } else
                                done = true;

                        }

                    }
                }

                int n1 = 0;
                int v1 = 0;
                int v2 = 0;
                int k2 = 0;

                boolean comment = false;
                for (int k1 = 0; k1 < b1.length; k1++) {
                    v1 = b1[k1];
                    if (v1 == 1) {
                        if (zriga.substring(k1, k1 + 1).equals(";")) {
                            comment = true;
                            break;
                        } else
                            comment = false;
                        break;
                    }
                }


                if (!comment) {
                    // cerca fino a che non trova n1 != n2;
                    //int lun = 0;
                    boolean again = true;
                    for (int k1 = 0; k1 < b1.length; k1++) {
                        v1 = b1[n1];
                        again = false;
                        for (k2 = n1 + 1; k2 < b1.length; k2++) {
                            v2 = b1[k2];
                            if (v2 != v1) {
                                again = true;
                                break;
                            }
                        }


                        //System.out.print("\n n1="+n1+" n2="+n2+" found elem ->"+elem+"<- size("+elem.length()+")");
                        k1 = k2;
                        n1 = k2;

                    }

                    riga = new StringTokenizer(zriga);

                    sz = riga.countTokens();
                    prev_word = null;
                    for (int r = 0; r < sz; r++) {
                        elem = riga.nextToken();
                        fnd = false;
                        for (int k = 0; k < My_keyword.length; k++) {
                            if (My_keyword[k].equalsIgnoreCase(elem)) {
                                fnd = true;
                                break;
                            }
                        }


                        if ((prev_word != null) && (prev_word.equalsIgnoreCase("data_from_file"))) {
                            if ((!comment) && elem.equalsIgnoreCase("Y")) {
                                EnvConstant.TYPE_OF_SIMULATION = EnvConstant.SIMULATION_FROM_FILE;
                            }
                        }


                        if ((prev_word != null) && (prev_word.equalsIgnoreCase("data_from_class"))) {
                            if ((!comment) && elem.equalsIgnoreCase("Y")) {
                                EnvConstant.TYPE_OF_SIMULATION = EnvConstant.SIMULATION_FROM_CLASS;
                            }
                        }

                        prev_word = elem;

                    }

                }


            }

        } catch (Exception e1) {
            logger.sendToStatus(" session: Couldn't insert initial text.:" + e1);
        }

    }


    public String[] convertToArray(String _text) {

        String s1 = _text;
        StringTokenizer riga;
        String elem;
        int sz;
        riga = new StringTokenizer(s1, "\n");
        sz = riga.countTokens();
        String[] source_new = new String[sz];

        for (int r = 0; r < sz; r++) {
            elem = (String) riga.nextToken();
            //   System.out.print("\n conv.to.string --> elem["+r+"] --> "+elem);
            source_new[r] = new String(elem + "\n");
        }
        return source_new;

    }


    public List<Double> getLastGenerationFitness() {
        return v1_fitness;
    }

    public List<Double> getLastGenerationWinnersFitness() {
        return v1_fitness_win;
    }

    public List<Double> getLastGenerationSpeciesSomething() {
        return v1_species;
    }
}
