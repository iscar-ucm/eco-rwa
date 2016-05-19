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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

public class FloorplanGeneticAsocMultiObj extends Problem<ComponentVariable> {

  private static final Logger LOGGER = Logger.getLogger(FloorplanGeneticAsocMultiObj.class.getName());
  protected FloorplanConfiguration cfg;
  protected boolean[][][] freeCells;
  protected int MaxWireLength = Integer.MAX_VALUE;

  public FloorplanGeneticAsocMultiObj(FloorplanConfiguration cfg) {
    super(cfg.components.size(), 3);
    this.cfg = cfg;
    freeCells = new boolean[cfg.maxLengthInCells][cfg.maxWidthInCells][cfg.numLayers];
    MaxWireLength = cfg.maxLengthInCells * cfg.maxWidthInCells * cfg.numLayers;
  }

  @Override
  public FloorplanGeneticAsocMultiObj clone() {
    FloorplanGeneticAsocMultiObj clone = new FloorplanGeneticAsocMultiObj(cfg.clone());
    return clone;
  }

  @Override
  public Solutions<ComponentVariable> newRandomSetOfSolutions(int size) {
    Solutions<ComponentVariable> solutions = new Solutions<ComponentVariable>();
    LinkedList<Component> componentsAsList = new LinkedList<Component>();
    for (Component c : cfg.components.values()) {
      componentsAsList.add(c);
    }
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
    for (Solution<ComponentVariable> solution : solutions) {
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

    double[] wireAndTemp = computeWireAndTemp(solution);
    solution.getObjectives().set(0, unfeasible);
    solution.getObjectives().set(1, wireAndTemp[0]);
    solution.getObjectives().set(2, wireAndTemp[1]);
  }

  public double fitnessWire(Solution<ComponentVariable> solution, int idx) {
    double fitness = 0.0;
    int idFrom, idTo;
    Component cI = solution.getVariables().get(idx).getValue();
    idFrom = cI.id;
    // Conexiones:
    Component cJ = null;
    for (int j = 0; j < idx; ++j) {
      cJ = solution.getVariables().get(j).getValue();
      idTo = cJ.id;
      if ((cfg.couplings.containsKey(idFrom) && cfg.couplings.get(idFrom).contains(idTo))
              || (cfg.couplings.containsKey(idTo) && cfg.couplings.get(idTo).contains(idFrom))) {
        fitness += Math.abs(cI.x + cI.l / 2 - cJ.x - cJ.l / 2) + Math.abs(cI.y + cI.w / 2 - cJ.y - cJ.w / 2) + Math.abs(cI.z - cJ.z);
      }
    }
    return fitness;
  }

  public double fitnessTemp(Solution<ComponentVariable> solution, int idx) {
    double fitness = 0.0;
    Component cI = solution.getVariables().get(idx).getValue();
    Component cJ = null;
    double dist = 0.0;
    for (int j = 0; j < idx; ++j) {
      cJ = solution.getVariables().get(j).getValue();
      dist = Math.sqrt(Math.pow(cI.x + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(cI.y + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(cI.z + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2));
      fitness += ((cI.dps[0] / cfg.maxDP) * (cJ.dps[0] / cfg.maxDP)) / dist;
    }
    return fitness;
  }

  public double[] computeWireAndTemp(Solution<ComponentVariable> solution) {
    boolean feasibleI = true, feasibleJ = true;
    double[] fitness = {0.0, 0.0};
    double dist = 0.0;


    int idFrom, idTo;
    ArrayList<ComponentVariable> variables = solution.getVariables();
    for (int i = 0; i < variables.size() - 1; ++i) {
      feasibleI = true;
      Component cI = variables.get(i).getValue();
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
            fitness[0] += Math.abs(cI.x + cI.l / 2 - cJ.x - cJ.l / 2) + Math.abs(cI.y + cI.w / 2 - cJ.y - cJ.w / 2) + Math.abs(cI.z - cJ.z);
          } else {
            fitness[0] += MaxWireLength;
          }
        }
        // Ahora Calculamos el impacto térmico:
        if (feasibleI && feasibleJ) {
          dist = Math.sqrt(Math.pow(cI.x + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(cI.y + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(cI.z + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2));
          fitness[1] += ((cI.dps[0] / cfg.maxDP) * (cJ.dps[0] / cfg.maxDP)) / dist;
        } else {
          fitness[1] += 1.0;
        }
      }
    }
    return fitness;
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
    if (args.length != 2) {
      System.out.println("Usage: java -jar FloorplanGeneticAsocMultiObj.jar XmlFilePath TimeInSeconds");
      System.out.println("Where:");
      System.out.println("XmlFilePath: Scenario path file (Absolute or relative)");
      System.out.println("TimeInSeconds: Execution time in seconds");
      return;
    }
    HeroLogger.setup(Level.INFO);
    String xmlFilePath = args[0];
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(new File(xmlFilePath.replaceAll(".xml", "MultiObj.txt"))));
    } catch (IOException ex) {
      Logger.getLogger(FloorplanGeneticAsocMultiObj.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long timeInMiliSeconds = 1000 * Long.valueOf(args[1]);
    Integer numIndi = 100;
    Integer numGene = 500000;

    FloorplanConfiguration cfg = new FloorplanConfiguration(xmlFilePath);
    FloorplanGeneticAsocMultiObj problem = new FloorplanGeneticAsocMultiObj(cfg);
    NSGAII<ComponentVariable> algorithm = new NSGAII<ComponentVariable>(problem, numIndi, numGene, new ComponentVariable.ComponentMutation(1.0 / problem.getNumberOfVariables()), new CycleCrossover<>(), new BinaryTournamentNSGAII<ComponentVariable>());

    LOGGER.info("Initializing ...");
    algorithm.initialize();
    LOGGER.info("Running ...");
    long startTime = System.currentTimeMillis();
    long endTime = startTime;

    Solutions<ComponentVariable> solutions = null;
    StringBuilder line = new StringBuilder();

    while (endTime - startTime < timeInMiliSeconds) {
      algorithm.step();
      endTime = System.currentTimeMillis();

      solutions = algorithm.getPopulation();
      try {
        line = new StringBuilder();
        for (Solution<ComponentVariable> solution : solutions) {
          line.append((endTime - startTime) / 1000.0);
          line.append(" ").append(solution.getObjectives().get(0));
          line.append(" ").append(solution.getObjectives().get(1));
          line.append(" ").append(solution.getObjectives().get(2));
          line.append("\n");
        }
        line.append("\n");
        LOGGER.info(line.toString());
        writer.write(line.toString());
        writer.flush();
      } catch (IOException ex) {
        Logger.getLogger(FloorplanGeneticAsocMultiObj.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    LOGGER.info("Done.");

    try {
      writer.close();
      solutions = algorithm.getCurrentSolution();
      problem.save(solutions, xmlFilePath);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
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
      String newXmlFilePathSuffix = "_" + this.getClass().getSimpleName() + "_" + solution.getObjectives().get(0) + "_" + solution.getObjectives().get(1) + "_" + solution.getObjectives().get(2) + ".xml";
      cfg.xmlFilePath = xmlFilePath.replaceAll(".xml", newXmlFilePathSuffix);
      cfg.save();
    }
  }
}
