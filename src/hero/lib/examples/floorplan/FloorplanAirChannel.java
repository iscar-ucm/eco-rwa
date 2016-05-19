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
package hero.lib.examples.floorplan;

import hero.core.algorithm.metaheuristic.moga.NSGAII;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import hero.core.operator.crossover.CycleCrossover;
import hero.core.operator.selection.BinaryTournamentNSGAII;
import hero.core.problem.Problem;
import hero.core.problem.Solution;
import hero.core.problem.Solutions;
import hero.core.util.logger.HeroLogger;
import hero.core.util.random.RandomGenerator;

public class FloorplanAirChannel extends Problem<ComponentVariable> {

  private static final Logger LOGGER = Logger.getLogger(FloorplanAirChannel.class.getName());
  public static final int OBJ_UNFEASIBLE = 0;
  public static final int OBJ_FIRST_TEMP = 1;
  protected FloorplanConfiguration cfg;
  protected int cellsInRegion;
  protected boolean[][][] freeCells;

  public FloorplanAirChannel(FloorplanConfiguration cfg, int cellsInRegion) {
    super(cfg.components.size(), FloorplanAirChannel.OBJ_FIRST_TEMP + cfg.numPowerProfiles);
    this.cfg = cfg;
    this.cellsInRegion = cellsInRegion;
    freeCells = new boolean[cfg.maxLengthInCells][cfg.maxWidthInCells][cfg.numLayers];
  }

  @Override
  public FloorplanAirChannel clone() {
    FloorplanAirChannel clone = new FloorplanAirChannel(cfg.clone(), cellsInRegion);
    return clone;
  }

  @Override
  public Solutions<ComponentVariable> newRandomSetOfSolutions(int size) {
    Solutions<ComponentVariable> solutions = new Solutions<>();
    LinkedList<Component> componentsAsList = new LinkedList<>();
    for (Component c : cfg.components.values()) {
      componentsAsList.add(c);
    }
    Collections.sort(componentsAsList, new ComponentThermalComparator());
    for (int i = 0; i < size; ++i) {
      Solution<ComponentVariable> solution = new Solution<>(super.numberOfObjectives);
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
    for (Solution<ComponentVariable> solution : solutions) {
      this.evaluate(solution);
    }
  }

  @Override
  public void evaluate(Solution<ComponentVariable> solution) {
    int x = 0, y = 0, z = 0;
    for (x = 0; x < cfg.maxLengthInCells; ++x) {
      for (y = 0; y < cfg.maxWidthInCells; ++y) {
        for (z = 0; z < cfg.numLayers; ++z) {
          freeCells[x][y][z] = true;
          if (z % 2 == 0) { // Región ubicada a la izquierda
            if (x == cellsInRegion || x == cellsInRegion + 1) {
              freeCells[x][y][z] = false;
            }
          } else { // Región ubicada a la derecha
            if (x == cfg.maxLengthInCells - cellsInRegion - 1 || x == cfg.maxLengthInCells - cellsInRegion - 2) {
              freeCells[x][y][z] = false;
            }
          }
        }
      }
    }
    double unfeasible = 0;
    ArrayList<ComponentVariable> variables = solution.getVariables();
    for (int i = 0; i < variables.size(); ++i) {
      unfeasible += place(solution, i);
    }

    double[] objs = computeTemp(solution);
    solution.getObjectives().set(FloorplanAirChannel.OBJ_UNFEASIBLE, unfeasible);
    for (int p = 0; p < cfg.numPowerProfiles; ++p) {
      solution.getObjectives().set(FloorplanAirChannel.OBJ_FIRST_TEMP + p, objs[p]);
    }
  }

  public double[] computeTemp(Solution<ComponentVariable> solution) {
    boolean feasibleI = true, feasibleJ = true;
    double[] objs = new double[cfg.numPowerProfiles];
    double dist = 0.0;

    ArrayList<ComponentVariable> variables = solution.getVariables();
    for (int i = 0; i < variables.size() - 1; ++i) {
      feasibleI = true;
      Component cI = variables.get(i).getValue();
      if (cI.x < 0 || cI.y < 0 || cI.z < 0) {
        feasibleI = false;
      }
      for (int j = i + 1; j < variables.size(); ++j) {
        feasibleJ = true;
        Component cJ = variables.get(j).getValue();
        if (cJ.x < 0 || cJ.y < 0 || cJ.z < 0) {
          feasibleJ = false;
        }
        // Calculamos el impacto térmico:
        if (feasibleI && feasibleJ) {
          dist = Math.sqrt(Math.pow(cI.x + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(cI.y + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(cI.z + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2));
          for (int p = 0; p < cfg.numPowerProfiles; ++p) {
            // Potencia: Minimiza temperaturas máximas
            objs[p] += ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP)) / dist;
            // Distancia: Minimiza temperaturas promedio
            //objs[p] -= dist / ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP));
          }
        } else {
          for (int p = 0; p < cfg.numPowerProfiles; ++p) {
            objs[p] += 1.0;
          }
        }
      }
    }
    return objs;
  }

  public double place(Solution<ComponentVariable> solution, int idx) {
    Component component = solution.getVariables().get(idx).getValue();
    boolean isCore = (component.type == Component.TYPE_CORE);
    int x = 0, y = 0, z = 0, bestX = -1, bestY = -1, bestZ = -1;
    double currentObj = Double.POSITIVE_INFINITY, bestObj = Double.POSITIVE_INFINITY;
    for (z = component.zMin; z <= component.zMax; ++z) {
      for (x = component.xMin; x <= component.xMax; ++x) {
        if (isCore) { // Metemos todos los cores en la región de aislamiento
          if (z % 2 == 0) { // A la izquierda en las capas pares
            if (x >= cellsInRegion) {
              continue;
            }
          } else { // A la derecha en las impares
            if (x < cfg.maxLengthInCells - cellsInRegion) {
              continue;
            }
          }
        }
        for (y = component.yMin; y <= component.yMax; ++y) {
          component.x = x;
          component.y = y;
          component.z = z;
          if (freeCells[x][y][z] && feasible(solution, idx)) {
            // Calculamos el objetivo
            currentObj = fitnessTemp(solution, idx);
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

  public boolean feasible(Solution<ComponentVariable> solution, int idx) {
    Component cI = solution.getVariables().get(idx).getValue();
    // Límites del chip
    if (cI.x + cI.l > cfg.maxLengthInCells) {
      return false;
    }
    if (cI.y + cI.w > cfg.maxWidthInCells) {
      return false;
    }
    if (cI.z + cI.h > cfg.numLayers) {
      return false;
    }
    // No puede cruzar un canal de aire
    if (cI.z % 2 == 0) { // Capas pares, el canal a la izquierda
      if (cI.x < cellsInRegion && cI.x + cI.l > cellsInRegion) {
        return false;
      }
    } else { // Capas impares, el canal a la derecha
      if (cI.x < cfg.maxLengthInCells - cellsInRegion - 2 && cI.x + cI.l > cfg.maxLengthInCells - cellsInRegion - 2) {
        return false;
      }
    }
    // No pueden superponerse dos componentes
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
    if (args.length < 2) {
      System.out.println("Usage: java -jar FloorplanAirChannel.jar -xml XmlFilePath -cell CellsInRegion [-saveRes SaveRes] [-numIndi NumIndi] [-numGene NumGene] [-seed Seed]");
      System.out.println("Where:");
      System.out.println("XmlFilePath: Scenario path file (Absolute or relative)");
      System.out.println("CellsInRegion: Number of cells used to define a region (air channel placement)");
      System.out.println("SaveRes: Save results. If 1 the non-dominated front is saved after the last generation. If 0, nothing is saved.");
      System.out.println("NumIndi: Number of individuals (100 by default)");
      System.out.println("NumGene: Number of generations (Max(250,NumComponents) by default)");
      System.out.println("Seed: The seed for the random number generator. If provided, it is used just in the last simulation");
      return;
    }
    HeroLogger.setup(Level.INFO);
    String xmlFilePath = null;
    Integer cellsInRegion = null;
    Integer saveRes = 0;
    Integer numIndi = null;
    Integer numGene = null;
    Long seed = null;
    for (int i = 0; i < args.length - 1; ++i) {
      if (args[i].equals("-xml")) {
        xmlFilePath = args[i + 1];
      } else if (args[i].equals("-cell")) {
        cellsInRegion = Integer.valueOf(args[i + 1]);
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
    FloorplanAirChannel problem = new FloorplanAirChannel(cfg, cellsInRegion);
    NSGAII<ComponentVariable> algorithm = new NSGAII<>(problem, numIndi, numGene, new ComponentVariable.ComponentMutation(1.0 / problem.getNumberOfVariables()), new CycleCrossover<>(), new BinaryTournamentNSGAII<>());
    LOGGER.info("Initializing ...");
    algorithm.initialize();
    double bestFirstObjective = Double.POSITIVE_INFINITY;
    int currentGeneration = 0;
    while (currentGeneration < numGene) {
      currentGeneration++;
      LOGGER.info("Running generation " + currentGeneration + "/" + numGene + "...");
      algorithm.step();
      Solutions<ComponentVariable> solutions = algorithm.getPopulation();
      for (Solution<ComponentVariable> solution : solutions) {
        if (solution.getObjectives().get(FloorplanGenetic.OBJ_UNFEASIBLE) < bestFirstObjective) {
          bestFirstObjective = solution.getObjectives().get(FloorplanGenetic.OBJ_UNFEASIBLE);
        }
      }
      LOGGER.info("Done. Best feasibility = " + bestFirstObjective + ".");
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
      String newXmlFilePathSuffix = "_" + this.getClass().getSimpleName() + "_" + solution.getObjectives().get(FloorplanAirChannel.OBJ_UNFEASIBLE);
      for (int p = 0; p < cfg.numPowerProfiles; ++p) {
        newXmlFilePathSuffix += "_" + solution.getObjectives().get(FloorplanAirChannel.OBJ_FIRST_TEMP + p);
      }
      newXmlFilePathSuffix += ".xml";
      cfg.xmlFilePath = xmlFilePath.replaceAll(".xml", newXmlFilePathSuffix);
      cfg.save();
    }
  }
}
