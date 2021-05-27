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
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import eco.core.algorithm.metaheuristic.moga.NSGAII;
import eco.core.operator.crossover.CycleCrossover;
import eco.core.operator.selection.BinaryTournamentNSGAII;
import eco.core.problem.Problem;
import eco.core.problem.Solution;
import eco.core.problem.Solutions;
import eco.core.util.logger.HeroLogger;
import eco.core.util.random.RandomGenerator;

public class FloorplanGenetic extends Problem<ComponentVariable> {

    private static final Logger LOGGER = Logger.getLogger(FloorplanGenetic.class.getName());
    public static final int OBJ_UNFEASIBLE = 0;
    public static final int OBJ_WIRELENGTH = 1;
    public static final int OBJ_FIRST_TEMP = 2;
    protected FloorplanConfiguration cfg;
    protected int MaxWireLength = Integer.MAX_VALUE;
    protected boolean[][][] freeCells;

    public FloorplanGenetic(FloorplanConfiguration cfg) {
        super(cfg.components.size(), FloorplanGenetic.OBJ_FIRST_TEMP + cfg.numPowerProfiles);
        this.cfg = cfg;
        MaxWireLength = cfg.maxLengthInCells * cfg.maxWidthInCells * cfg.numLayers;
        freeCells = new boolean[cfg.maxLengthInCells][cfg.maxWidthInCells][cfg.numLayers];
    }

  @Override
    public FloorplanGenetic clone() {
        FloorplanGenetic clone = new FloorplanGenetic(cfg.clone());
        return clone;
    }

    @Override
    public Solutions<ComponentVariable> newRandomSetOfSolutions(int size) {
      Solutions<ComponentVariable> solutions = new Solutions<ComponentVariable>();
        LinkedList<Component> componentsAsList = new LinkedList<Component>();
        for (Component c : cfg.components.values()) {
            componentsAsList.add(c);
        }
        Collections.sort(componentsAsList, new ComponentThermalComparator());        
        for (int i = 0; i < size; ++i) {
            Solution<ComponentVariable> solution = new Solution<ComponentVariable>(super.numberOfObjectives);
            for (Component component : componentsAsList) {
                solution.getVariables().add(new ComponentVariable(component.clone()));
            }
            solutions.add(solution);
            Collections.shuffle(componentsAsList);
        }
        return solutions;
    }

  @Override
    public void evaluate(Solutions<ComponentVariable> solutions) {
      for(Solution<ComponentVariable> solution : solutions) {
        evaluate(solution);
      }
    }
    public void evaluate(Solution<ComponentVariable> solution) {
        int x = 0, y = 0, z = 0;
        for (x = 0; x < cfg.maxLengthInCells; ++x) {
            for (y = 0; y < cfg.maxWidthInCells; ++y) {
                for (z = 0; z < cfg.numLayers; ++z) {
                    freeCells[x][y][z] = true;
                }
            }
        }
        double unfeasible = 0;
        ArrayList<ComponentVariable> variables = solution.getVariables();
        for (int i = 0; i < variables.size(); ++i) {
            unfeasible += place(solution, i);
        }

        double[] objs = computeWireAndTemp(solution);
        solution.getObjectives().set(FloorplanGenetic.OBJ_UNFEASIBLE, unfeasible);
        solution.getObjectives().set(FloorplanGenetic.OBJ_WIRELENGTH, objs[0]);
        for (int p = 0; p < cfg.numPowerProfiles; ++p) {
            solution.getObjectives().set(FloorplanGenetic.OBJ_FIRST_TEMP + p, objs[FloorplanGenetic.OBJ_WIRELENGTH + p]);
        }
    }

    public double fitnessWire(Solution<ComponentVariable> solution, int idx) {
        double fitness = 0.0;
        int idFrom, idTo;
        int xLI, xRI, xLJ, xRJ, yUI, yDI, yUJ, yDJ, dx, dy, dz;
        Component cI = solution.getVariables().get(idx).getValue();
        idFrom = cI.id;
        xLI = cI.x;
        xRI = cI.x + cI.l;
        yUI = cI.y;
        yDI = cI.y + cI.w;
        // Conexiones:
        Component cJ = null;
        for (int j = 0; j < idx; ++j) {
            cJ = solution.getVariables().get(j).getValue();
            idTo = cJ.id;
            if ((cfg.couplings.containsKey(idFrom) && cfg.couplings.get(idFrom).contains(idTo))
                    || (cfg.couplings.containsKey(idTo) && cfg.couplings.get(idTo).contains(idFrom))) {
                xLJ = cJ.x;
                xRJ = cJ.x + cJ.l;
                yUJ = cJ.y;
                yDJ = cJ.y + cJ.w;
                dz = Math.abs(cI.z - cJ.z);
                if (dz > 0) { // Prohibimos esta opción
                    fitness += MaxWireLength;
                } else { // Para cablear tomamos distancias más cortas entre bordes más próximos.
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
                    fitness += dx + dy;
                }
            }
        }
        return fitness;
    }

    public double fitnessTemp(Solution<ComponentVariable> solution, int idx) {
        double[] tempObjs = new double[cfg.numPowerProfiles];
        for (int i = 0; i < tempObjs.length; ++i) {
            tempObjs[i] = 0.0;
        }
        Component cI = solution.getVariables().get(idx).getValue();
        Component cJ = null;
        double dist = 0.0;
        for (int j = 0; j < idx; ++j) {
            cJ = solution.getVariables().get(j).getValue();
            dist = Math.sqrt(Math.pow(cI.x + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(cI.y + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(cI.z + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2));
            //if (dist != 0) { -> SE SUPONE QUE DIST es != 0
            for (int p = 0; p < tempObjs.length; ++p) {
                // Potencia: Minimiza temperaturas máximas
                tempObjs[p] += ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP)) / dist;
                // Distancia: Minimiza temperaturas promedio
                //tempObjs[p] -= dist / ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP));
            }
            //}
        }
        double fitness = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < tempObjs.length; ++i) {
            if (tempObjs[i] > fitness) {
                fitness = tempObjs[i];
            }
        }
        return fitness;
    }

    public double[] computeWireAndTemp(Solution<ComponentVariable> solution) {
        boolean feasibleI = true, feasibleJ = true;
        double[] objs = new double[cfg.numPowerProfiles + 1];
        double dist = 0.0;

        int xLI, xRI, xLJ, xRJ, yUI, yDI, yUJ, yDJ, dx, dy, dz;
        int idFrom, idTo;
        ArrayList<ComponentVariable> variables = solution.getVariables();
        for (int i = 0; i < variables.size() - 1; ++i) {
            feasibleI = true;
            Component cI = variables.get(i).getValue();
            xLI = cI.x;
            xRI = cI.x + cI.l;
            yUI = cI.y;
            yDI = cI.y + cI.w;
            if (cI.x < 0 || cI.y < 0 || cI.z < 0) {
                feasibleI = false;
            }
            idFrom = cI.id;
            for (int j = i + 1; j < variables.size(); ++j) {
                feasibleJ = true;
                Component cJ = variables.get(j).getValue();
                if (cJ.x < 0 || cJ.y < 0 || cJ.z < 0) {
                    feasibleJ = false;
                }
                idTo = cJ.id;
                // Calculamos el cableado:
                if ((cfg.couplings.containsKey(idFrom) && cfg.couplings.get(idFrom).contains(idTo))
                        || (cfg.couplings.containsKey(idTo) && cfg.couplings.get(idTo).contains(idFrom))) {
                    if (feasibleI && feasibleJ) {
                        xLJ = cJ.x;
                        xRJ = cJ.x + cJ.l;
                        yUJ = cJ.y;
                        yDJ = cJ.y + cJ.w;
                        dz = Math.abs(cI.z - cJ.z);
                        if (dz > 0) { // Prohibimos esta opción
                            objs[0] += MaxWireLength;
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
                            objs[0] += dx + dy;
                        }
                    } else {
                        objs[0] += MaxWireLength;
                    }
                }
                // Ahora Calculamos el impacto térmico:
                if (feasibleI && feasibleJ) {
                    dist = Math.sqrt(Math.pow(cI.x + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(cI.y + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(cI.z + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2));
                    for (int p = 0; p < cfg.numPowerProfiles; ++p) {
                        // Potencia: Minimiza temperaturas máximas
                        objs[p + 1] += ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP)) / dist;
                        // Distancia: Minimiza temperaturas promedio
                        //objs[p + 1] -= dist / ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP));
                    }
                } else {
                    for (int p = 0; p < cfg.numPowerProfiles; ++p) {
                        objs[p + 1] += 1.0;
                    }
                }
            }
        }
        return objs;
    }

    public double place(Solution<ComponentVariable> solution, int idx) {
        Component component = solution.getVariables().get(idx).getValue();
        int x = 0, y = 0, z = 0, bestX = -1, bestY = -1, bestZ = -1;
        double currentObj = Double.POSITIVE_INFINITY, bestObj = Double.POSITIVE_INFINITY;
        for (z = component.zMin; z <= component.zMax; ++z) {
            for (x = component.xMin; x <= component.xMax; ++x) {
                for (y = component.yMin; y <= component.yMax; ++y) {
                    component.x = x;
                    component.y = y;
                    component.z = z;
                    if (freeCells[x][y][z] && feasible(solution, idx)) {
                        // Calculamos el objetivo
                        if (component.type == Component.TYPE_CORE) {
                            currentObj = fitnessTemp(solution, idx);
                        } else {
                            currentObj = fitnessWire(solution, idx);
                        }
                        if (currentObj < bestObj) {
                            bestObj = currentObj;
                            bestX = x;
                            bestY = y;
                            bestZ = z;
                        }
                    }
                }
            }
        }
        component.x = bestX;
        component.y = bestY;
        component.z = bestZ;
        if (bestX < 0 || bestY < 0 || bestZ < 0) {
            return 1.0;
        }
        for (x = component.x; x < component.x + component.l; ++x) {
            for (y = component.y; y < component.y + component.w; ++y) {
                freeCells[x][y][bestZ] = false;
            }
        }
        return 0.0;
    }

    public boolean feasible(Solution<ComponentVariable> solution, int idx) {
        Component cI = solution.getVariables().get(idx).getValue();
        // Límites del chip
        if (cI.x + cI.l > cfg.maxLengthInCells) {
            return false;
            //return (xx + component.l - maxLength);
        }
        if (cI.y + cI.w > cfg.maxWidthInCells) {
            return false;
            //return (yy + component.w - maxWidth);
        }
        if (cI.z + cI.h > cfg.numLayers) {
            return false;
            //return (zz + component.h - maxHeight);
        }
        // No puede haber nada de por medio
        Component cJ = null;
        double dx = 0, dy = 0;
        for (int j = 0; j < idx; ++j) {
            cJ = solution.getVariables().get(j).getValue();
            if (cJ.z != cI.z) {
                continue;
            }
            dx = Math.abs(cJ.x + cJ.l / 2.0 - cI.x - cI.l / 2.0);
            dy = Math.abs(cJ.y + cJ.w / 2.0 - cI.y - cI.w / 2.0);
            if (dx < (cJ.l / 2.0 + cI.l / 2.0) && dy < (cJ.w / 2.0 + cI.w / 2.0)) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar FloorplanGenetic.jar -xml XmlFilePath [-saveRes SaveRes] [-numIndi NumIndi] [-numGene NumGene] [-seed Seed]");
            System.out.println("Where:");
            System.out.println("XmlFilePath: Scenario path file (Absolute or relative)");
            System.out.println("SaveRes: Save results. If 1 the non-dominated front is saved after the last generation. If 2 the population is saved after each generation, and the non-dominated front is saved at the end");
            System.out.println("NumIndi: Number of individuals (100 by default)");
            System.out.println("NumGene: Number of generations (Max(250,NumComponents) by default)");
            System.out.println("Seed: The seed for the random number generator. If provided, it is used just in the last simulation");
            return;
        }
        HeroLogger.setup(Level.INFO);
        String xmlFilePath = null;
        Integer saveRes = 0;
        Integer numIndi = null;
        Integer numGene = null;
        Long seed = null;
        for (int i = 0; i < args.length - 1; ++i) {
            if (args[i].equals("-xml")) {
                xmlFilePath = args[i + 1];
            } else if (args[i].equals("-saveRes")) {
                saveRes = Integer.valueOf(args[i + 1]);
            } else if (args[i].equals("-numIndi")) {
                numIndi = Integer.valueOf(args[i + 1]);
            } else if (args[i].equals("-numGene")) {
                numGene = Integer.valueOf(args[i + 1]);
            } else if (args[i].equals("-seed")) {
                seed = Long.valueOf(args[i + 1]);
            }
        }
        if (seed != null) {
            RandomGenerator.setSeed(seed);
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
        FloorplanGenetic problem = new FloorplanGenetic(cfg);
        NSGAII<ComponentVariable> algorithm = new NSGAII<ComponentVariable>(problem, numIndi, numGene, new ComponentVariable.ComponentMutation(1.0 / problem.getNumberOfVariables()), new CycleCrossover<ComponentVariable>(), new BinaryTournamentNSGAII<ComponentVariable>());
        LOGGER.info("Initializing ...");
        algorithm.initialize();
        int currentGeneration = 0;
        while (currentGeneration < numGene) {
            currentGeneration++;
            LOGGER.info("Running generation " + currentGeneration + "/" + numGene + "...");
            algorithm.step();
            if (saveRes == 2) {
                try {
                    String newXmlFilePath = xmlFilePath.replaceAll(".xml", "Gen" + currentGeneration + ".xml");
                    problem.save(algorithm.getPopulation(), newXmlFilePath);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
        if (saveRes > 0) {
            Solutions<ComponentVariable> solutions = algorithm.getCurrentSolution();
            try {
                problem.save(solutions, xmlFilePath);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

    }

    public void save(Solutions<ComponentVariable> solutions, String xmlFilePath) throws IOException {
        for (Solution<ComponentVariable> solution : solutions) {
            ArrayList<ComponentVariable> variables = solution.getVariables();
            for (int i = 0; i < variables.size(); ++i) {
                Component c = variables.get(i).getValue();
                if (c.id < 0) // Es un operador
                {
                    continue;
                }
                Component cCfg = cfg.components.get(c.id);
                cCfg.x = c.x;
                cCfg.xMin = c.x;
                cCfg.xMax = c.x;
                cCfg.y = c.y;
                cCfg.yMin = c.y;
                cCfg.yMax = c.y;
                cCfg.z = c.z;
                cCfg.zMin = c.z;
                cCfg.zMax = c.z;
                cCfg.l = c.l;
                cCfg.w = c.w;
                cCfg.h = c.h;
            }
            String newXmlFilePathSuffix = "_" + this.getClass().getSimpleName() + "_" + solution.getObjectives().get(FloorplanGenetic.OBJ_UNFEASIBLE) + "_" + solution.getObjectives().get(FloorplanGenetic.OBJ_WIRELENGTH);
            for (int p = 0; p < cfg.numPowerProfiles; ++p) {
                newXmlFilePathSuffix += "_" + solution.getObjectives().get(FloorplanGenetic.OBJ_FIRST_TEMP + p);
            }
            newXmlFilePathSuffix += ".xml";
            cfg.xmlFilePath = xmlFilePath.replaceAll(".xml", newXmlFilePathSuffix);
            cfg.save();
        }
    }

}
