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
import java.awt.Point;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import hero.core.operator.crossover.SinglePointCrossover;
import hero.core.operator.mutation.BooleanMutation;
import hero.core.operator.selection.BinaryTournamentNSGAII;
import hero.core.problem.Problem;
import hero.core.problem.Solution;
import hero.core.problem.Solutions;
import hero.core.problem.Variable;

public class FloorplanThermalViasAux extends FloorplanTsv {
    public static int OBJ_BoundaryTsvs = 2;

    public FloorplanThermalViasAux(FloorplanConfiguration configuration) {
        super(configuration);
        super.numberOfObjectives = super.numberOfObjectives + 1;
    }

    @Override
    public void evaluate(Solution<Variable<Boolean>> solution) {
        super.evaluate(solution);
        // We count the number of TSVs in the boundary:
        double num = 0.0;
        for(int i=0; i<solution.getVariables().size(); ++i) {
            if(solution.getVariables().get(i).getValue()) {
                Point p = allowedPoints.get(i);
                if(p.x == conf.maxLengthInCells-1) {
                    num++;
                }
            }
        }
        solution.getObjectives().set(OBJ_BoundaryTsvs, num);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Parameters: xmlFilePath");
            return;
        }

        String xmlFilePath = args[0];
        FloorplanConfiguration conf = new FloorplanConfiguration(xmlFilePath);
        Integer numIndi = 100;
        Integer numGene = 2500;
        if (100 * conf.components.size() > numGene) {
            numGene = 100 * conf.components.size();
        }

        FloorplanThermalViasAux thermalVias = new FloorplanThermalViasAux(conf);
        NSGAII<Variable<Boolean>> nsga2 = new NSGAII<Variable<Boolean>>(thermalVias, numIndi, numGene, new BooleanMutation<Variable<Boolean>>(1.0 / thermalVias.getNumberOfVariables()), new SinglePointCrossover<Variable<Boolean>>(thermalVias), new BinaryTournamentNSGAII<Variable<Boolean>>());
        nsga2.initialize();
        Solutions<Variable<Boolean>> solutions = nsga2.execute();
        Logger.getLogger(FloorplanThermalViasAux.class.getName()).info(solutions.toString());

        try {
            for (Solution<Variable<Boolean>> solution : solutions) {
                conf.thermalVias.clear();
                for (int i = 0; i < conf.numLayers - 1; ++i) {
                    for (int j = thermalVias.startIndex[i]; j < thermalVias.endIndex[i]; ++j) {
                        if (solution.getVariables().get(j).getValue()) {//If it is a TSV
                            Point point = thermalVias.allowedPoints.get(j);
                            ThermalVia thermalVia = new ThermalVia(conf.numLayers - 1, i, point.x, point.y);
                            conf.thermalVias.add(thermalVia);
                        }
                    }
                }
                conf.xmlFilePath = xmlFilePath.replaceAll(".xml", "") + "_" + FloorplanThermalViasAux.class.getSimpleName() + "_" + solution.getObjectives().get(FloorplanThermalViasAux.OBJ_NumThermalVias) + "_" + solution.getObjectives().get(FloorplanThermalViasAux.OBJ_WireLength) + "_" + solution.getObjectives().get(FloorplanThermalViasAux.OBJ_BoundaryTsvs) + ".xml";
                conf.save();
            }
        } catch (IOException ex) {
            Logger.getLogger(FloorplanThermalViasAux.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Problem<Variable<Boolean>> clone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
