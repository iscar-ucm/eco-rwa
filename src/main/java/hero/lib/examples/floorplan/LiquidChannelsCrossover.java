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

import hero.core.operator.crossover.CrossoverOperator;
import hero.core.problem.Solution;
import hero.core.problem.Solutions;
import hero.core.problem.Variable;
import hero.core.util.random.RandomGenerator;

public class LiquidChannelsCrossover extends CrossoverOperator<Variable<Boolean>> {

    protected double probability;
    protected int maxNumLcs;

    public LiquidChannelsCrossover(double probability, int maxNumLcs) {
        super();
        this.probability = probability;
        this.maxNumLcs = maxNumLcs;
    }

  @Override
    public Solutions<Variable<Boolean>> execute(Solution<Variable<Boolean>> parent1, Solution<Variable<Boolean>> parent2) {
        Solution<Variable<Boolean>> child1, child2;
        child1 = parent1.clone();
        child2 = parent2.clone();
        int idx = RandomGenerator.nextInt(parent1.getVariables().size());
        int numLcsInChild1 = 0;
        int numLcsInChild2 = 0;
        for (int i = 0; i <= idx; ++i) {
            if (child1.getVariables().get(i).getValue()) {
                numLcsInChild1++;
            }
            if (child2.getVariables().get(i).getValue()) {
                numLcsInChild2++;
            }
        }
        for (int i = idx + 1; i < parent1.getVariables().size(); ++i) {
            Variable<Boolean> varPar1 = parent1.getVariables().get(i).clone();
            Variable<Boolean> varPar2 = parent2.getVariables().get(i).clone();
            if (numLcsInChild1 >= maxNumLcs) {
                varPar2.setValue(Boolean.FALSE);
            } else {
                if (varPar2.getValue()) {
                    numLcsInChild1++;
                }
            }
            child1.getVariables().set(i, varPar2);
            if (numLcsInChild2 >= maxNumLcs) {
                varPar1.setValue(Boolean.FALSE);
            } else {
                if (varPar1.getValue()) {
                    numLcsInChild2++;
                }
            }
            child2.getVariables().set(i, varPar1);
        }

        while (numLcsInChild1 < maxNumLcs) {
            idx = RandomGenerator.nextInt(child1.getVariables().size());
            Variable<Boolean> variable = child1.getVariables().get(idx);
            if (!variable.getValue()) {
                variable.setValue(Boolean.TRUE);
                numLcsInChild1++;
            }
        }

        while (numLcsInChild2 < maxNumLcs) {
            idx = RandomGenerator.nextInt(child2.getVariables().size());
            Variable<Boolean> variable = child2.getVariables().get(idx);
            if (!variable.getValue()) {
                variable.setValue(Boolean.TRUE);
                numLcsInChild2++;
            }
        }

        Solutions<Variable<Boolean>> offspring = new Solutions<>();
        offspring.add(child1);
        offspring.add(child2);
        return offspring;
    }
} // Crossover operator

