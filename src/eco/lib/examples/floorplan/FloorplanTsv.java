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

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import eco.core.algorithm.metaheuristic.moga.NSGAII;
import eco.core.operator.crossover.SinglePointCrossover;
import eco.core.operator.mutation.BooleanMutation;
import eco.core.operator.selection.BinaryTournamentNSGAII;
import eco.core.problem.Problem;
import eco.core.problem.Solution;
import eco.core.problem.Solutions;
import eco.core.problem.Variable;
import eco.core.util.logger.HeroLogger;

public class FloorplanTsv extends Problem<Variable<Boolean>> {

    public static int OBJ_NumThermalVias = 0;
    public static int OBJ_WireLength = 1;
    protected FloorplanConfiguration conf;
    protected ArrayList<Point> allowedPoints;
    protected int startIndex[];
    protected int endIndex[];
    protected int numberOfCores;
    protected int numberOfCouplings;
    protected double MaxWireLength = Double.POSITIVE_INFINITY;

    public FloorplanTsv(FloorplanConfiguration configuration) {
        super(0, 2);
        this.conf = configuration;
        allowedPoints = new ArrayList<Point>();
        numberOfCores = 0;
        initialize();
        super.numberOfVariables += allowedPoints.size();
        MaxWireLength = conf.numLayers * conf.maxLengthInCells * conf.maxWidthInCells;
    }

    public final void initialize() {
        // BUSY VARIABLE;
        boolean busy[][][] = new boolean[conf.maxLengthInCells][conf.maxWidthInCells][conf.numLayers];
        int xMin = 0, yMin = 0, zMin = 0;
        int xMax = conf.maxLengthInCells, yMax = conf.maxWidthInCells, zMax = conf.numLayers;
        for (int x = xMin; x < xMax; ++x) {
            for (int y = yMin; y < yMax; ++y) {
                for (int z = zMin; z < zMax; ++z) {
                    busy[x][y][z] = false;
                }
            }
        }
        // NUMBER OF CORES AND STUFF
        Collection<Component> components = conf.components.values();
        for (Component component : components) {
            if (component.type == Component.TYPE_CORE) {
                numberOfCores++;
            }
            xMin = component.x;
            xMax = component.x + component.l;
            yMin = component.y;
            yMax = component.y + component.w;
            zMin = component.z;
            zMax = component.z + component.h;
            for (int x = xMin; x < xMax; ++x) {
                for (int y = yMin; y < yMax; ++y) {
                    for (int z = zMin; z < zMax; ++z) {
                        busy[x][y][z] = true;
                    }
                }
            }
        }
        // NUMBER OF COUPLINGS
        numberOfCouplings = conf.couplings.values().size();

        // ALLOWED POINTS
        xMin = 0;
        yMin = 0;
        zMin = 0;
        xMax = conf.maxLengthInCells;
        yMax = conf.maxWidthInCells;
        zMax = conf.numLayers - 1;
        // Array of allowed TSVs, one per pair.
        // For example, if there are 4 layers (numbered from 0 to 3),
        // we have 3 pairs to create TSVs:
        // - 3-0 (later 3 to layer 0), 3-1 (later 3 to layer 1), 3-2 (later 3 to layer 2)
        // TODO: This algorithm can be improved because allowedTSVs(3-0) \in allowedTSVs(3-1) \in allowedTSVs(3-2).
        startIndex = new int[zMax];
        endIndex = new int[zMax];
        boolean allowed;
        for (int i = 0; i < zMax; ++i) {
            startIndex[i] = (i == 0) ? 0 : endIndex[i - 1];
            endIndex[i] = startIndex[i];
            for (int x = xMin; x < xMax; ++x) {
                for (int y = yMin; y < yMax; ++y) {
                    allowed = !busy[x][y][zMax];
                    for (int z = i; allowed && z < zMax; ++z) {
                        allowed = allowed && !busy[x][y][z];
                    }
                    if (allowed) {
                        allowedPoints.add(new Point(x, y));
                        endIndex[i]++;
                    }
                }
            }
        }

        // Report:
        Logger.getLogger(FloorplanTsv.class.getName()).log(Level.INFO, "Number of cores = " + numberOfCores);
        Logger.getLogger(FloorplanTsv.class.getName()).log(Level.INFO, "Number of couplings = " + numberOfCouplings);
        for (int i = 0; i < zMax; ++i) {
            int num = 0;
            for (int j = startIndex[i]; j < endIndex[i]; ++j) {
                num++;
            }
            Logger.getLogger(FloorplanTsv.class.getName()).info("Allowed TSVs connecting layer " + zMax + " with layer " + i + " = " + num);
        }
    }

    public LinkedList<Point> getAllowedThermalViasToLevel(int level) {
        LinkedList<Point> thermalVias = new LinkedList<Point>();
        for (int i = startIndex[level]; i < endIndex[level]; ++i) {
            thermalVias.add(allowedPoints.get(i));
        }
        return thermalVias;
    }

  @Override
    public Solutions<Variable<Boolean>> newRandomSetOfSolutions(int size) {
      Solutions<Variable<Boolean>> solutions = new Solutions<Variable<Boolean>>();
        ArrayList<Integer> idxs = new ArrayList<Integer>();
        for (int i = 0; i < numberOfVariables; ++i) {
            idxs.add(i);
        }
        for (int i=0; i<size; ++i) {
          Solution<Variable<Boolean>> solution = new Solution<Variable<Boolean>>(super.numberOfObjectives);
          for(int j=0; j<numberOfVariables; ++j) {
            solution.getVariables().add(new Variable<Boolean>(false));
          }
            Collections.shuffle(idxs);
            for (int j = 0; j < numberOfVariables; ++j) {
                int idx = idxs.get(j);
                boolean varJ = false;
                if (j < numberOfCores) {
                    varJ = true;
                }
                solution.getVariables().set(idx, new Variable<Boolean>(varJ));
            }
            solutions.add(solution);
        }
        return solutions;
    }
  
  @Override
  public void evaluate(Solutions<Variable<Boolean>> solutions) {
    for(Solution<Variable<Boolean>> solution : solutions) {
      evaluate(solution);
    }
  }
    
    public void evaluate(Solution<Variable<Boolean>> solution) {
        // First objective, number of TSVs
        int num = 0;
        for (int i = 0; i < super.numberOfVariables; ++i) {
            if (solution.getVariables().get(i).getValue()) {
                num++;
            }
        }
        solution.getObjectives().set(0, 1.0 * num);

        // Second objective, wire length
        // Important: The maximum number of TSVs is equal to 2*NumberOfCores
        if (num > 2 * numberOfCores) {
            solution.getObjectives().set(1, numberOfCouplings * MaxWireLength);
            return;
        }

        int xLI, xRI, xLJ, xRJ, yUI, yDI, yUJ, yDJ, dx, dy, dz;
        double wireLength = 0.0;
        Set<Integer> idsFrom = conf.couplings.keySet();
        for (int idFrom : idsFrom) {
            Component cI = conf.components.get(idFrom);
            xLI = cI.x;
            xRI = cI.x + cI.l;
            yUI = cI.y;
            yDI = cI.y + cI.w;
            HashSet<Integer> idsTo = conf.couplings.get(idFrom);
            for (int idTo : idsTo) {
                Component cJ = conf.components.get(idTo);
                xLJ = cJ.x;
                xRJ = cJ.x + cJ.l;
                yUJ = cJ.y;
                yDJ = cJ.y + cJ.w;
                dz = Math.abs(cI.z - cJ.z);
                // If they are in the same layer we compute Manhattan distance
                if (dz > 0) {
                    // We must find the best TSV connecting both layers:
                    double dist = findBestTSV(cI, cJ, solution);
                    if (dist < 0) { // Prohibimos esta opción
                        wireLength += MaxWireLength;
                    } else {
                        wireLength += dist;
                    }
                } else {
                    if ((xLI >= xLJ && xLI <= xRJ) || (xRI >= xLJ && xRI <= xRJ)) {
                        dx = 0;
                    } else {
                        dx = Math.min(Math.abs(xLI - xLJ), Math.abs(xLI - xRJ));
                        dx = Math.min(Math.abs(xRI - xLJ), dx);
                        dx = Math.min(Math.abs(xRI - xRJ), dx);
                    }
                    if ((yUI >= yUJ && yUI <= yDJ) || (yDI >= yUJ && yDI <= yDJ)) {
                        dy = 0;
                    } else {
                        dy = Math.min(Math.abs(yUI - yUJ), Math.abs(yUI - yDJ));
                        dy = Math.min(Math.abs(yDI - yUJ), dy);
                        dy = Math.min(Math.abs(yDI - yDJ), dy);
                    }
                    wireLength += dx + dy;
                }
            }
        }
        solution.getObjectives().set(1, wireLength);
    }

    public double findBestTSV(Component cI, Component cJ, Solution<Variable<Boolean>> solution) {
        double dist, distMin = Double.POSITIVE_INFINITY;
        boolean found = false;
        int zMin = Math.min(cI.z, cJ.z);
        // I must find the starting index in the chromosome:
        boolean isTSV = false;
        for (int i = 0; i < endIndex[zMin]; ++i) {
            isTSV = solution.getVariables().get(i).getValue();
            if (isTSV) {
                Point point = allowedPoints.get(i);
                dist = Math.min(Math.abs(cI.x - point.x), Math.abs(cI.x + cI.l - point.x))
                        + Math.min(Math.abs(cI.y - point.y), Math.abs(cI.y + cI.w - point.y))
                        + Math.abs((cI.z - cJ.z))
                        + Math.min(Math.abs(cJ.x - point.x), Math.abs(cJ.x + cJ.l - point.x))
                        + Math.min(Math.abs(cJ.y - point.y), Math.abs(cJ.y + cJ.w - point.y));
                if (dist < distMin) {
                    distMin = dist;
                }
                found = true;
            }
        }
        return (found) ? distMin : -1;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Parameters: xmlFilePath");
            return;
        }
        HeroLogger.setup(Level.INFO);

        String xmlFilePath = args[0];
        FloorplanConfiguration conf = new FloorplanConfiguration(xmlFilePath);
        Integer numIndi = 100;
        Integer numGene = 2500;
        if (100 * conf.components.size() > numGene) {
            numGene = 100 * conf.components.size();
        }

        FloorplanTsv floorplanTsv = new FloorplanTsv(conf);
        NSGAII<Variable<Boolean>> nsga2 = new NSGAII<Variable<Boolean>>(floorplanTsv, numIndi, numGene, new BooleanMutation<Variable<Boolean>>(1.0 / floorplanTsv.getNumberOfVariables()), new SinglePointCrossover<Variable<Boolean>>(floorplanTsv), new BinaryTournamentNSGAII<Variable<Boolean>>());
        nsga2.initialize();
        Solutions<Variable<Boolean>> solutions = nsga2.execute();

        try {
            for (Solution<Variable<Boolean>> solution : solutions) {
                conf.thermalVias.clear();
                for (int i = 0; i < conf.numLayers - 1; ++i) {
                    for (int j = floorplanTsv.startIndex[i]; j < floorplanTsv.endIndex[i]; ++j) {
                        if (solution.getVariables().get(j).getValue()) {//If it is a TSV
                            Point point = floorplanTsv.allowedPoints.get(j);
                            ThermalVia thermalVia = new ThermalVia(conf.numLayers - 1, i, point.x, point.y);
                            conf.thermalVias.add(thermalVia);
                        }
                    }
                }
                conf.xmlFilePath = xmlFilePath.replaceAll(".xml", "") + "_" + floorplanTsv.getClass().getSimpleName() + "_" + solution.getObjectives().get(FloorplanTsv.OBJ_NumThermalVias) + "_" + solution.getObjectives().get(FloorplanTsv.OBJ_WireLength) + ".xml";
                conf.save();
            }
        } catch (IOException ex) {
            Logger.getLogger(FloorplanTsv.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Problem<Variable<Boolean>> clone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
