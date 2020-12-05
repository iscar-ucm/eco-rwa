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
package hero.lib.examples.floorplan.register;

import hero.core.operator.mutation.MutationOperator;
import hero.core.problem.Solution;
import hero.core.problem.Variable;
import hero.core.util.random.RandomGenerator;

/**
 *
 * @author jlrisco
 */
public class RegisterVariable extends Variable<Register> {

  public static class RegisterMutation extends MutationOperator<RegisterVariable> {

    public RegisterMutation(double probability) {
      super(probability);
    }

    @Override
    public Solution<RegisterVariable> execute(Solution<RegisterVariable> object) {
      Solution<RegisterVariable> solution = object;
      int j;
      Register tempComp;
      RegisterVariable varI, varJ;
      int size = solution.getVariables().size();
      for (int i = 0; i < size; ++i) {
        if (RandomGenerator.nextDouble() < probability) {
          varI = solution.getVariables().get(i);
          // Swap two blocks:
          j = RandomGenerator.nextInt(size);
          varJ = solution.getVariables().get(j);
          tempComp = varI.value;
          varI.value = varJ.value;
          varJ.value = tempComp;
        }
      }
      return solution;
    }
  } // Mutation operator

  public RegisterVariable(Register component) {
    super(component);
  }

  @Override
  public RegisterVariable clone() {
    RegisterVariable clone = new RegisterVariable(this.value.clone());
    return clone;
  }

  @Override
  public boolean equals(Object obj) {
    RegisterVariable right = (RegisterVariable) obj;
    if (value.id == right.value.id) {
      return true;
    }
    return false;

  }

  @Override
  public String toString() {
    return value.toString();
  }
}
