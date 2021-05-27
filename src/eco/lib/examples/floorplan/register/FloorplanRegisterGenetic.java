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
package eco.lib.examples.floorplan.register;

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

public class FloorplanRegisterGenetic extends Problem<RegisterVariable> {

    private static final Logger logger = Logger.getLogger(FloorplanRegisterGenetic.class.getName());
    protected FloorplanRegisterConfiguration cfg;
    protected boolean feasibleSolutionFound = false;

    public FloorplanRegisterGenetic(FloorplanRegisterConfiguration cfg) {
        super(cfg.components.size(), 1 + cfg.numPowerProfiles);
        this.cfg = cfg;
    }

  @Override
    public FloorplanRegisterGenetic clone() {
        FloorplanRegisterGenetic clone = new FloorplanRegisterGenetic(cfg.clone());
        return clone;
    }

    @Override
    public Solutions<RegisterVariable> newRandomSetOfSolutions(int size) {
      Solutions<RegisterVariable> solutions = new Solutions<RegisterVariable>();
        LinkedList<Register> registersAsList = new LinkedList<Register>();
        for (Register register : cfg.components) {
            registersAsList.add(register);
        }
        for (int i = 0; i < size; ++i) {
            Solution<RegisterVariable> solution = new Solution<RegisterVariable>(super.numberOfObjectives);
            for (Register register : registersAsList) {
                solution.getVariables().add(new RegisterVariable(register.clone()));
            }
            solutions.add(solution);
            Collections.shuffle(registersAsList);
        }
        return solutions;
    }
    
  @Override
    public void evaluate(Solutions<RegisterVariable> solutions) {
      for(Solution<RegisterVariable> solution : solutions) {
        this.evaluate(solution);
      }
    }

    public void evaluate(Solution<RegisterVariable> solution) {
        double unfeasible = 0;
        unfeasible += place(solution);
        if(!feasibleSolutionFound && unfeasible==0) {
          feasibleSolutionFound = true;
          logger.info("Feasible solution found.");
        }
        solution.getObjectives().set(0, unfeasible);
        double[] objs = fitnessTemp(solution);
        for (int p = 0; p < cfg.numPowerProfiles; ++p) {
            solution.getObjectives().set(1 + p, (unfeasible+1) * objs[p]);
        }
    }

    public double[] fitnessTemp(Solution<RegisterVariable> solution) {
        double[] objs = new double[cfg.numPowerProfiles];
        double dist = 0.0;

        ArrayList<RegisterVariable> variables = solution.getVariables();
        for (int i = 0; i < variables.size() - 1; ++i) {
            Register cI = variables.get(i).getValue();
            for (int j = i + 1; j < variables.size(); ++j) {
                Register cJ = variables.get(j).getValue();
                // Ahora Calculamos el impacto térmico:
                dist = Math.sqrt(Math.pow(cI.x + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(cI.y + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2));
                if (dist > 0) {
                    for (int p = 0; p < cfg.numPowerProfiles; ++p) {
                        objs[p] += ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP)) / dist;
                    }
                }
            }
        }
        return objs;
    }

    public double place(Solution<RegisterVariable> solution) {
        double unfeasible = 0.0;
        ArrayList<RegisterVariable> registerVariables = solution.getVariables();
        for(int i=0; i<registerVariables.size(); ++i) {
            Register register = registerVariables.get(i).getValue();
            Register registerOriginal = cfg.components.get(i);
            register.x = registerOriginal.x;
            register.y = registerOriginal.y;
            if(register.x<register.xMin || register.x>register.xMax || register.y<register.yMin || register.y>register.yMax)
                unfeasible++;
        }
        return unfeasible;
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Parameters: XmlFilePath NumIndividuals NumGenerations SaveResults(0/1)");
            args = new String[4];
            args[0] = "G:\\jlrisco\\Trabajo\\Investiga\\Estudiantes\\MaríaJosé\\Floorplanning\\gzip7_64x1_free.xml";
            args[1] = "100";
            args[2] = "250";
            args[3] = "1";
            return;
        }
        HeroLogger.setup(Level.INFO);
        String xmlFilePath = args[0];
        Integer numIndi = Integer.valueOf(args[1]);
        Integer numGene = Integer.valueOf(args[2]);
        Integer saveRes = Integer.valueOf(args[3]);

        FloorplanRegisterConfiguration cfg = new FloorplanRegisterConfiguration(xmlFilePath);
        FloorplanRegisterGenetic problem = new FloorplanRegisterGenetic(cfg);
        NSGAII<RegisterVariable> algorithm = new NSGAII<RegisterVariable>(problem, numIndi, numGene, new RegisterVariable.RegisterMutation(1.0 / problem.getNumberOfVariables()), new CycleCrossover<RegisterVariable>(), new BinaryTournamentNSGAII<RegisterVariable>());
        long start = System.currentTimeMillis();
        algorithm.initialize();
        Solutions<RegisterVariable> solutions = algorithm.execute();
        long stop = System.currentTimeMillis();
        logger.info("Time: " + ((stop - start)/1000.0) + " seconds.");

        if (saveRes > 0) {
            try {
                problem.save(solutions, xmlFilePath);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    public void save(Solutions<RegisterVariable> solutions, String xmlFilePath) throws IOException {
        for (Solution<RegisterVariable> solution : solutions) {
            ArrayList<RegisterVariable> variables = solution.getVariables();
            for (int i = 0; i < variables.size(); ++i) {
                cfg.components.set(i, variables.get(i).getValue());
            }
            String newXmlFilePathSuffix = "_" + FloorplanRegisterGenetic.class.getSimpleName() + "_" + solution.getObjectives().get(0);
            for (int p = 0; p < cfg.numPowerProfiles; ++p) {
                newXmlFilePathSuffix += "_" + solution.getObjectives().get(1 + p);
            }
            newXmlFilePathSuffix += ".xml";
            cfg.xmlFilePath = xmlFilePath.replaceAll(".xml", newXmlFilePathSuffix);
            cfg.save();
        }
    }

}
