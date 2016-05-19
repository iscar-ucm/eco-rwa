/*
 * Copyright (C) 2010-2016 José Luis Risco Martín <jlrisco@ucm.es>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
  *  - José Luis Risco Martín
 */
package hero.lib.examples.floorplan.util;

import hero.lib.examples.floorplan.FloorplanSolution;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class implementing the simulated annealing technique for problem
 * solving.
 *
 * Does not require temperature to be given because it automatically
 * adapts the parameters: Natural Optimization [de Vicente et al., 2000]
 *
 * @author J. M. Colmenar
 */
public class SimulatedAnnealingSolver extends AbstractSolver {


    /** Maximum number of iterations */
    private long maxIterations = 10000;
    private long currentMoves = 0;
    private boolean findFeasible = false;
    /** Maximum time in seconds. If 0, then consider maxIterations */
    private long maxSeconds = 0;

    /* Cost-related attributes */
    private double currentMinimumCost = Double.MAX_VALUE;
    private double initialCost = Double.MAX_VALUE;
    private double k = 1.0;

    /** Random number generator */
    private static Random rnd;

    /** Logger */
    private static final Logger logger = Logger.getLogger(SimulatedAnnealingSolver.class.getName());

    /** Nome of the log file */
    public static String logFile = "objectives_log.txt";


    /** This constructor allows to establish the maximum number of
     * iterations.
     *
     * @param maxIter number of iterations where the search will stop.
     * @param  randomSeed seed for random number generation
     */
    public SimulatedAnnealingSolver(Long maxIter, Long randomSeed) {
        super();
        maxIterations = maxIter;
        rnd = new Random(randomSeed);
    }

    /** Parameterized constructor
     *
     * @param maxIter number of iterations where the search will stop.
     * @param k is the weight of the temperature
     * @param stopWhenFeasible stops the search if finds a feasible solution
     * @param randomSeed seed for random number generation
     * @param maxSecs is maximum execution time in seconds. If 0, then consider max iterations
     */
    public SimulatedAnnealingSolver(Long maxIter, double k, Boolean stopWhenFeasible, Long randomSeed, Long maxSecs) {
        this(maxIter,randomSeed);
        this.k = k;
        findFeasible = stopWhenFeasible;
        maxSeconds = maxSecs;
    }

    @Override
    protected void search(Solution initial) {

        Solution bestSol = initial;
        AbstractSolver.bestSolution = initial.clone();

        // Log algorithm parameters:
        
        final int LOG_RATIO = 1000;
        
        // Clean log file:
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(logFile)));
            writer.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }   
        
        // Logging SA parameters
        String logStr = "\n# SA Parameters:";
        logStr +="\n-> K (weight for temperature): "+k;
        logStr +="\n-> Max. iterations: "+maxIterations;
        logStr +="\n-> Max. time: "+maxSeconds+" seconds";
        logStr +="\n-> Stop when feasible: "+findFeasible;
        logStr +="\n";

        logger.log(Level.INFO, logStr);

        initialCost = initial.getObjective();

        double startTime = System.currentTimeMillis();
        // Log starting point:
        logObjectives(0,bestSol);
        
        boolean stopSA = false;
        boolean change = false;
        long numChanges = 0;

        while ( !stopSA && (!findFeasible || (findFeasible && !bestSol.isFeasible())) ) {
            currentMinimumCost = bestSol.getObjective();

            // Obtain a neighbour (next state)
            Solution newSolution = bestSol.getSuccessors().nextElement();

            currentMoves++;

            /* Compute neighbour's (state) energy and check if move to
             the neighbour (state) */

            /* If new solution is has best objetive value, change */
            if (newSolution.compareTo(bestSol) < 0) {
                change = true;
            } else {
                // If new solution is worse, change depends on probability
                if (changeState(bestSol,newSolution))
                    change = true;
            }

            double time = (System.currentTimeMillis() - startTime)/1000.0;

            if (change) {
                numChanges++;
                bestSol = newSolution.clone();
                AbstractSolver.bestSolution = bestSol;
                // Txt for objectives
                logObjectives(time,bestSol);
                // Logs detail only if solution changes and following the ratio
                if ((numChanges % LOG_RATIO) == 0) {
                    // Screen and also backups solution to XML file
                    logStr = "\n# SA -- Iterations: "+currentMoves+" -- Current SA Temperature: "+Double.toString(getTemperature())+"\n";
                    logStr += "-- Current Best Solution: "+bestSol+"\n";
                    logStr += "Time: " + time + " seconds.\n";
                    logger.log(Level.INFO, logStr);
                }
                change = false;
            }     

            // On screen log, also backups configuration to XML file
            

            if (maxSeconds == 0) {
                stopSA = (currentMoves == maxIterations);
            } else {
                stopSA = time >= maxSeconds;
            }
        }

        double finalTime = (System.currentTimeMillis() - startTime)/1000.0;
        logObjectives(finalTime,bestSol);
        logStr = "\n# TOTAL SA -- Iterations: "+currentMoves+" -- Current SA Temperature: "+Double.toString(getTemperature())+"\n";
        logStr += "TOTAL Time: " + finalTime + " seconds.\n";
        logger.log(Level.INFO,logStr);

    }

    /**
     * Computes probability of changing to new solution
     *
     * @param oldSol current state
     * @param newSol possible next state
     * @return true if probability gives chance to change state, false otherwise
     */
    private boolean changeState(Solution oldSol, Solution newSol) {

        // Higher cost means new energy to be higher than old energy
        double energyDiff = newSol.getObjective() - oldSol.getObjective();

        // Compute probability. Must be between 0 and 1.
        double temp = getTemperature();
        double prob = Math.exp(-energyDiff/temp);

        // nextDouble returns the next pseudorandom, uniformly distributed double value between 0.0 and 1.0
        if (rnd.nextDouble() <= prob)
            return true;
        else
            return false;
    }


    /**
     * Obtains the temperature, which is naturally adapted to evolution
     * of the search.
     */
    private double getTemperature() {
         return k * Math.abs((currentMinimumCost - initialCost) / currentMoves);

    }

    private void logObjectives(double time, Solution bestSol) {
        if (bestSol instanceof FloorplanSolution) {
            ((FloorplanSolution) bestSol).logObjectives(time,logFile);
        } else {
            // File log code
            try {
                // Appending
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(logFile),true));

                // # Time Obj.Value Feasible
                writer.write(time + " " + bestSol.getObjective() + " "+(bestSol.isFeasible() ? "1":"0")+"\n");
                writer.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }                        
        }
    }

}

