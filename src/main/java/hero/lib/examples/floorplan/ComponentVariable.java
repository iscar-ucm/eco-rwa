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

import hero.core.operator.mutation.MutationOperator;
import hero.core.problem.Solution;
import hero.core.problem.Variable;
import hero.core.util.random.RandomGenerator;

/**
 *
 * @author jlrisco
 */
public class ComponentVariable extends Variable<Component> {

  public static class ComponentMutation extends MutationOperator<ComponentVariable> {

    public ComponentMutation(double probability) {
      super(probability);
    }

    @Override
    public Solution<ComponentVariable> execute(Solution<ComponentVariable> object) {
      Solution<ComponentVariable> solution = object;
      int temp, j;
      double random;
      Component tempComp;
      ComponentVariable varI, varJ;
      int size = solution.getVariables().size();
      for (int i = 0; i < size; ++i) {
        if (RandomGenerator.nextDouble() < probability) {
          varI = solution.getVariables().get(i);
          random = RandomGenerator.nextDouble();
          if (random < 0.50) {
            // Mutation type 0 - Swap two blocks:
            j = RandomGenerator.nextInt(size);
            varJ = solution.getVariables().get(j);
            tempComp = varI.value;
            varI.value = varJ.value;
            varJ.value = tempComp;
          } else {
            // Change the length-width:
            varI.value.xMax += varI.value.l - varI.value.w;
            varI.value.yMax += varI.value.w - varI.value.l;
            temp = varI.value.l;
            varI.value.l = varI.value.w;
            varI.value.w = temp;
          }
        }
      }
      return solution;
    }
  } // Mutation operator

  public ComponentVariable(Component component) {
    super(component);
  }

  @Override
  public ComponentVariable clone() {
    ComponentVariable clone = new ComponentVariable(this.value.clone());
    return clone;
  }

  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public boolean equals(Object obj) {
    ComponentVariable right = (ComponentVariable) obj;
    if (value.id == right.value.id) {
      return true;
    }
    return false;

  }
}
