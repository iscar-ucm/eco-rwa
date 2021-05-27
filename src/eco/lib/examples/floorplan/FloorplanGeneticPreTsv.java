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
package eco.lib.examples.floorplan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import eco.core.algorithm.metaheuristic.moga.NSGAII;
import eco.core.operator.crossover.CycleCrossover;
import eco.core.operator.selection.BinaryTournamentNSGAII;
import eco.core.problem.Solution;
import eco.core.problem.Solutions;
import eco.core.util.logger.HeroLogger;

public class FloorplanGeneticPreTsv extends FloorplanGenetic {

    private static final Logger logger = Logger.getLogger(FloorplanGeneticPreTsv.class.getName());

    public FloorplanGeneticPreTsv(FloorplanConfiguration cfg) {
        super(cfg);
    }

    @Override
    public FloorplanGeneticPreTsv clone() {
        FloorplanGeneticPreTsv clone = new FloorplanGeneticPreTsv(cfg.clone());
        return clone;
    }

    @Override
    public void evaluate(Solution<ComponentVariable> solution) {
        super.evaluate(solution);

        // ALLOWED POINTS
        // Array of allowed TSVs, one per pair.
        // For example, if there are 4 layers (numbered from 0 to 3),
        // we have 3 pairs to create TSVs:
        // - 3-0 (later 3 to layer 0), 3-1 (later 3 to layer 1), 3-2 (later 3 to layer 2)
        // TODO: This algorithm can be improved because allowedTSVs(3-0) \in allowedTSVs(3-1) \in allowedTSVs(3-2).
        int xMin = 0, yMin = 0;
        int xMax = cfg.maxLengthInCells, yMax = cfg.maxWidthInCells, zMax = cfg.numLayers - 1;
        int[] startIndex = new int[zMax];
        int[] endIndex = new int[zMax];
        boolean allowed;
        for (int i = 0; i < zMax; ++i) {
            startIndex[i] = (i == 0) ? 0 : endIndex[i - 1];
            endIndex[i] = startIndex[i];
            for (int x = xMin; x < xMax; ++x) {
                for (int y = yMin; y < yMax; ++y) {
                    allowed = freeCells[x][y][zMax];
                    for (int z = i; allowed && z < zMax; ++z) {
                        allowed = allowed && freeCells[x][y][z];
                    }
                    if (allowed) {
                        endIndex[i]++;
                    }
                }
            }
        }

        // FEASIBILITY
        // We count the number of couplings that cannot be routed using TSVs:
        int unfeasible = 0;
        int idFrom, idTo;
        ArrayList<ComponentVariable> variables = solution.getVariables();
        for (int i = 0; i < variables.size() - 1; ++i) {
            Component cI = variables.get(i).getValue();
            idFrom = cI.id;
            for (int j = i + 1; j < variables.size(); ++j) {
                Component cJ = variables.get(j).getValue();
                idTo = cJ.id;
                // Calculamos el cableado:
                if ((cfg.couplings.containsKey(idFrom) && cfg.couplings.get(idFrom).contains(idTo))
                        || (cfg.couplings.containsKey(idTo) && cfg.couplings.get(idTo).contains(idFrom))) {
                    // If they are in different layers we must find 1 TSV at least
                    if (cI.z != cJ.z) {
                        int zMin = Math.min(cI.z, cJ.z);
                        if (zMin < 0 || endIndex[zMin] <= 0) {
                            unfeasible++;
                        }
                    }
                }
            }
        }
        if (unfeasible > 0) {
            solution.getObjectives().set(FloorplanGenetic.OBJ_UNFEASIBLE, unfeasible + solution.getObjectives().get(FloorplanGenetic.OBJ_UNFEASIBLE));
            solution.getObjectives().set(FloorplanGenetic.OBJ_WIRELENGTH, unfeasible * MaxWireLength + solution.getObjectives().get(FloorplanGenetic.OBJ_WIRELENGTH));
            for (int p = 0; p < cfg.numPowerProfiles; ++p) {
                solution.getObjectives().set(p + FloorplanGenetic.OBJ_FIRST_TEMP, unfeasible + solution.getObjectives().get(p + FloorplanGenetic.OBJ_FIRST_TEMP));
            }
            logger.fine("Number of connections that cannot be routed with a TSV: " + unfeasible);
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Parameters: -f XmlFilePath [-n NumberOfInvididuals] [-g NumberOfGenerations] -s SaveResults(0/1)");
            args = new String[4];
            args[0] = "-f";
            args[1] = "D:\\jlrisco\\Trabajo\\Investiga\\Estudiantes\\DavidCuesta\\benchmarks\\ComparaTavgVsTmax\\NiagaraC48L4.xml";
            args[2] = "-s";
            args[3] = "0";
            return;
        }
        HeroLogger.setup(Level.INFO);
        String xmlFilePath = null;
        Integer saveRes = 0;
        Integer numIndi = null;
        Integer numGene = null;
        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-f")) {
                xmlFilePath = args[i + 1];
            } else if (args[i].equals("-s")) {
                saveRes = Integer.valueOf(args[i + 1]);
            } else if (args[i].equals("-n")) {
                numIndi = Integer.valueOf(args[i + 1]);
            } else if (args[i].equals("-g")) {
                numGene = Integer.valueOf(args[i + 1]);
            }
        }


        FloorplanConfiguration cfg = new FloorplanConfiguration(xmlFilePath);
        if (numIndi == null) {
            numIndi = 100;
        }
        if (numGene == null) {
            numGene = 250;
            if (cfg.components.size() > numGene) {
                numGene = cfg.components.size();
            }
        }
        FloorplanGeneticPreTsv problem = new FloorplanGeneticPreTsv(cfg);
        NSGAII<ComponentVariable> algorithm = new NSGAII<ComponentVariable>(problem, numIndi, numGene, new ComponentVariable.ComponentMutation(1.0 / problem.getNumberOfVariables()), new CycleCrossover<ComponentVariable>(), new BinaryTournamentNSGAII<ComponentVariable>());
        logger.info("Initializing ...");
        algorithm.initialize();
        double bestFirstObjective = Double.POSITIVE_INFINITY;
        int currentGeneration = 0;
        while (currentGeneration < numGene) {
            currentGeneration++;
            logger.info("Running generation " + currentGeneration + "/" + numGene + "...");
            algorithm.step();
            Solutions<ComponentVariable> solutions = algorithm.getPopulation();
            for (Solution<ComponentVariable> solution : solutions) {
                if (solution.getObjectives().get(FloorplanGenetic.OBJ_UNFEASIBLE) < bestFirstObjective) {
                    bestFirstObjective = solution.getObjectives().get(FloorplanGenetic.OBJ_UNFEASIBLE);
                }
            }
            logger.info("Done. Best feasibility = " + bestFirstObjective + ".");
            if (bestFirstObjective > 0 && numGene - currentGeneration <= 5 && numGene < 10 * cfg.components.size()) {
                numGene++;
            }
        }
        if (saveRes > 0) {
            try {
                problem.save(algorithm.getCurrentSolution(), xmlFilePath);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }
}
