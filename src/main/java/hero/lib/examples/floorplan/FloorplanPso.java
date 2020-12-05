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

import hero.core.algorithm.metaheuristic.mopso.OMOPSO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import hero.core.problem.Problem;
import hero.core.problem.Solution;
import hero.core.problem.Solutions;
import hero.core.problem.Variable;
import hero.core.util.logger.HeroLogger;
import hero.core.util.random.RandomGenerator;

/**
 * Trying to solve the Floorplanning problem using PSO.
 * The algorithm does not work. It takes too much time till find a feasible solution.
 * If we set one of the particles equal to the Niagara3 original floorplanning it works
 * (more or less). However, it does not seem to improve previous existing algorithms.
 * @author jlrisco
 */

public class FloorplanPso extends Problem<Variable<Double>> {

  private static final Logger logger = Logger.getLogger(FloorplanPso.class.getName());
  public static final int OBJ_UnFeasible = 0;
  public static final int OBJ_WireLength = 1;
  public static final int OBJ_FirstTemp = 2;
  protected double bestFeasibility = Double.MAX_VALUE;
  protected double bestTemperature = Double.MAX_VALUE;
  protected FloorplanConfiguration cfg;
  protected ArrayList<Component> components = new ArrayList<Component>();
  protected boolean[][] feasibleIJ;
  protected int MaxWireLength = Integer.MAX_VALUE;

  public FloorplanPso(FloorplanConfiguration cfg) {
    super(3 * cfg.components.size(), FloorplanPso.OBJ_FirstTemp + cfg.numPowerProfiles);
    this.cfg = cfg;
    for (Component c : cfg.components.values()) {
      components.add(c.clone());
    }
    feasibleIJ = new boolean[components.size()][components.size()];
    Component c = null;
    for (int i = 0; i < components.size(); i++) {
      c = components.get(i);
      lowerBound[3 * i] = c.xMin;
      upperBound[3 * i] = c.xMax;
      lowerBound[3 * i + 1] = c.yMin;
      upperBound[3 * i + 1] = c.yMax;
      lowerBound[3 * i + 2] = c.zMin;
      upperBound[3 * i + 2] = c.zMax;
    }
    MaxWireLength = cfg.maxLengthInCells * cfg.maxWidthInCells * cfg.numLayers;
  }

  @Override
  public FloorplanPso clone() {
    FloorplanPso clone = new FloorplanPso(cfg.clone());
    return clone;
  }

  @Override
  public Solutions<Variable<Double>> newRandomSetOfSolutions(int size) {
    Solutions<Variable<Double>> solutions = new Solutions<Variable<Double>>();
    for (int i=0; i<size; ++i) {
        Solution<Variable<Double>> solution = new Solution<Variable<Double>>(super.numberOfObjectives);
      for (int j = 0; j < numberOfVariables; ++j) {
        Variable<Double> var = new Variable<Double>(RandomGenerator.nextDouble(lowerBound[j], upperBound[j]));
        solution.getVariables().add(var);
      }
      solutions.add(solution);
    }
    Solution<Variable<Double>> solution = solutions.get(0);
    for(int i=0; i<components.size(); ++i) {
      Component c = components.get(i);
      solution.getVariables().get(3*i).setValue(1.0*c.x);
      solution.getVariables().get(3*i+1).setValue(1.0*c.y);
      solution.getVariables().get(3*i+2).setValue(1.0*c.z);
    }
    return solutions;
  }
  
  @Override
  public void evaluate(Solutions<Variable<Double>> solutions) {
    for(Solution<Variable<Double>> solution : solutions) {
      evaluate(solution);
    }
  }

  public void evaluate(Solution<Variable<Double>> solution) {
    Component c = null;
    for (int i = 0; i < components.size(); ++i) {
      c = components.get(i);
      c.x = (int) Math.round(solution.getVariables().get(3 * i).getValue());
      c.y = (int) Math.round(solution.getVariables().get(3 * i + 1).getValue());
      c.z = (int) Math.round(solution.getVariables().get(3 * i + 2).getValue());
    }
    double unfeasible = feasibility();
    double[] objs = computeWireAndTemp();
    solution.getObjectives().set(FloorplanPso.OBJ_UnFeasible, unfeasible);
    solution.getObjectives().set(FloorplanPso.OBJ_WireLength, objs[0]);
    for (int p = 0; p < cfg.numPowerProfiles; ++p) {
      solution.getObjectives().set(FloorplanPso.OBJ_FirstTemp + p, objs[FloorplanPso.OBJ_WireLength + p]);
    }
    if (unfeasible < bestFeasibility) {
      bestFeasibility = unfeasible;
      logger.info("Best solution found: " + solution.toString());
    }
    if (unfeasible==0 && objs[1] < bestTemperature) {
      bestTemperature = objs[1];
      logger.info("Best temperature found: " + solution.toString());
    }

  }

  public double[] computeWireAndTemp() {
    double[] fitness = {0.0, 0.0};
    double dist = 0.0;

    int idFrom, idTo;
    for (int i = 0; i < components.size() - 1; ++i) {
      Component cI = components.get(i);
      idFrom = cI.id;
      for (int j = i + 1; j < components.size(); ++j) {
        Component cJ = components.get(j);
        idTo = cJ.id;
        // Calculamos el cableado:
        if ((cfg.couplings.containsKey(idFrom) && cfg.couplings.get(idFrom).contains(idTo))
                || (cfg.couplings.containsKey(idTo) && cfg.couplings.get(idTo).contains(idFrom))) {
          if (feasibleIJ[i][i] && feasibleIJ[j][j] && feasibleIJ[i][j]) {
            fitness[0] += Math.abs(cI.x + cI.l / 2 - cJ.x - cJ.l / 2) + Math.abs(cI.y + cI.w / 2 - cJ.y - cJ.w / 2) + Math.abs(cI.z - cJ.z);
          } else {
            fitness[0] += MaxWireLength;
          }
        }
        // Ahora Calculamos el impacto térmico:
        if (feasibleIJ[i][i] && feasibleIJ[j][j] && feasibleIJ[i][j]) {
          dist = Math.sqrt(Math.pow(cI.x + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(cI.y + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(cI.z + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2));
          fitness[1] += ((cI.dps[0] / cfg.maxDP) * (cJ.dps[0] / cfg.maxDP)) / dist;
        } else {
          fitness[1] += 1.0;
        }
      }
    }
    return fitness;
  }

  public double feasibility() {
    for (int i = 0; i < components.size(); ++i) {
      for (int j = 0; j < components.size(); ++j) {
        feasibleIJ[i][j] = true;
      }
    }

    double res = 0.0;

    Component cI = null, cJ = null;
    for (int i = 0; i < components.size(); ++i) {
      cI = components.get(i);
      // Límites del chip
      if (cI.x + cI.l > cfg.maxLengthInCells) {
        res += cI.x + cI.l - cfg.maxLengthInCells;
        feasibleIJ[i][i] = false;
      }
      if (cI.y + cI.w > cfg.maxWidthInCells) {
        res += cI.y + cI.w - cfg.maxWidthInCells;;
        feasibleIJ[i][i] = false;
      }
      if (cI.z + cI.h > cfg.numLayers) {
        res += cI.z + cI.h - cfg.numLayers;
        feasibleIJ[i][i] = false;
      }
      // No puede haber nada de por medio
      double dx = 0, dy = 0;
      for (int j = 0; j < i; ++j) {
        cJ = components.get(j);
        if (cJ.z != cI.z) {
          continue;
        }
        dx = Math.abs(cJ.x + cJ.l / 2.0 - cI.x - cI.l / 2.0);
        dy = Math.abs(cJ.y + cJ.w / 2.0 - cI.y - cI.w / 2.0);
        if (dx < (cJ.l / 2.0 + cI.l / 2.0) && dy < (cJ.w / 2.0 + cI.w / 2.0)) {
          res += (cJ.l / 2.0 + cI.l / 2.0) + (cJ.w / 2.0 + cI.w / 2.0) - dx - dy;
          feasibleIJ[i][j] = false;
          feasibleIJ[j][i] = false;
        }
      }
    }
    return res;
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: java -jar FloorplanGenetic.jar -xml XmlFilePath [-numIndi NumIndi] [-numGene NumGene] [-seed Seed]");
      System.out.println("Where:");
      System.out.println("XmlFilePath: Scenario path file (Absolute or relative)");
      System.out.println("NumIndi: Number of individuals (100 by default)");
      System.out.println("NumGene: Number of generations (Max(250,NumComponents) by default)");
      System.out.println("Seed: The seed for the random number generator. If provided, it is used just in the last simulation");
      //return;
      args = new String[6];
      args[0] = "-xml";
      args[1] = "D:\\jlrisco\\TrabajoExtra\\Dropbox\\Temporal\\2012_ASOC\\N3HC48.xml";
      args[2] = "-numIndi";
      args[3] = "100";
      args[4] = "-numGene";
      args[5] = "2000";
    }
    HeroLogger.setup(Level.INFO);
    String xmlFilePath = null;
    Integer numIndi = null;
    Integer numGene = null;
    Long seed = null;
    for (int i = 0; i < args.length - 1; ++i) {
      if (args[i].equals("-xml")) {
        xmlFilePath = args[i + 1];
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
    FloorplanPso problem = new FloorplanPso(cfg);
    OMOPSO<Variable<Double>> algorithm = new OMOPSO<Variable<Double>>(problem, numIndi, numGene);
    logger.info("Initializing ...");
    algorithm.initialize();
    Solutions<Variable<Double>> solutions = algorithm.execute();
    try {
      problem.save(solutions, xmlFilePath);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
  }

  public void save(Solutions<Variable<Double>> solutions, String xmlFilePath) throws IOException {
    for (Solution<Variable<Double>> solution : solutions) {
      Component c = null;
      for (int i = 0; i < components.size(); ++i) {
        c = components.get(i);
        c.x = (int) Math.round(solution.getVariables().get(3 * i).getValue());
        c.y = (int) Math.round(solution.getVariables().get(3 * i + 1).getValue());
        c.z = (int) Math.round(solution.getVariables().get(3 * i + 2).getValue());
      }
      for (int i = 0; i < components.size(); ++i) {
        c = components.get(i);
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
      String newXmlFilePathSuffix = "_" + this.getClass().getSimpleName() + "_" + solution.getObjectives().get(FloorplanPso.OBJ_UnFeasible) + "_" + solution.getObjectives().get(FloorplanPso.OBJ_WireLength);
      for (int p = 0; p < cfg.numPowerProfiles; ++p) {
        newXmlFilePathSuffix += "_" + solution.getObjectives().get(FloorplanPso.OBJ_FirstTemp + p);
      }
      newXmlFilePathSuffix += ".xml";
      cfg.xmlFilePath = xmlFilePath.replaceAll(".xml", newXmlFilePathSuffix);
      cfg.save();
    }
  }
}
