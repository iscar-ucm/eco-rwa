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
public class PostfixVariable extends Variable<Component> {

  public static class PostfixMutation extends MutationOperator<PostfixVariable> {

    public PostfixMutation(double probability) {
      super(probability);
    }

    @Override
    public Solution<PostfixVariable> execute(Solution<PostfixVariable> object) {
      Solution<PostfixVariable> solution = object;
      int size = solution.getVariables().size();
      for (int i = 0; i < size; ++i) {
        if (RandomGenerator.nextDouble() < probability) {
          PostfixVariable varI = solution.getVariables().get(i);
          double random = RandomGenerator.nextDouble();
          if (random < 0.25) {
            // Mutation type 0 - Swap two blocks:
            int j = RandomGenerator.nextInt(size);
            PostfixVariable varJ = solution.getVariables().get(j);
            Component temp = varI.value;
            varI.value = varJ.value;
            varJ.value = temp;
          } else if (random < 0.50) {
            // Change the chain type:
            StringBuilder chainTemp = new StringBuilder();
            chainTemp.append(varI.chain);
            for (int j = 0; j < varI.chain.length(); j++) {
              chainTemp.replace(j, j + 1, CHAIN_OPTIONS[RandomGenerator.nextInt(CHAIN_OPTIONS.length)]);
            }
            varI.chain = chainTemp.toString();
          } else if (random < 0.75) {
            // Change the chain length:
            int chainLength = varI.chain.length();
            int oldChainLength = chainLength;
            if (RandomGenerator.nextDouble() < 0.5) {
              chainLength = chainLength + 1;
              if (chainLength > varI.maxChainLength) {
                chainLength = varI.maxChainLength;
              }
            } else {
              chainLength = chainLength - 1;
              if (chainLength < 0) {
                chainLength = 0;
              }
            }

            if (chainLength > oldChainLength) {
              StringBuilder chainTemp = new StringBuilder();
              chainTemp.append(varI.chain);
              for (int j = oldChainLength; j < chainLength; ++j) {
                chainTemp.append(CHAIN_OPTIONS[RandomGenerator.nextInt(CHAIN_OPTIONS.length)]);
              }
              varI.chain = chainTemp.toString();
            } else if (chainLength < oldChainLength) {
              varI.chain = varI.chain.substring(0, chainLength);
            }
          } else if (random < 1.00) {
            // Change the length-width:
            varI.value.xMax += varI.value.l - varI.value.w;
            varI.value.yMax += varI.value.w - varI.value.l;
            Integer temp = varI.value.l;
            varI.value.l = varI.value.w;
            varI.value.w = temp;
          }
        }
      }
      return solution;
    }
  } // Mutation operator
  public static int DEFAULT_MAX_CHAIN_LENGTH = 5;
  protected int maxChainLength;
  protected String chain;
  protected static String[] CHAIN_OPTIONS = {"H", "V", "Z"};

  public PostfixVariable(Component component, int maxChainLength) {
    super(component);
    this.maxChainLength = maxChainLength;
    int chainLength = 0;
    if (RandomGenerator.nextDouble() >= 0.5) {
      chainLength = poisson(5);
    }

    if (chainLength > maxChainLength) {
      chainLength = maxChainLength;
    }
    StringBuilder chainTemp = new StringBuilder();
    for (int i = 0; i < chainLength; ++i) {
      chainTemp.append(CHAIN_OPTIONS[RandomGenerator.nextInt(CHAIN_OPTIONS.length)]);
    }
    chain = chainTemp.toString();
  }
  
  public void resizeChain(int newSize) {
    if(newSize < chain.length())
      chain = chain.substring(0, newSize);
    else if (newSize > chain.length()) {
      StringBuilder chainTemp = new StringBuilder();
      chainTemp.append(chain);
      while(chainTemp.length() < newSize)
        chainTemp.append(CHAIN_OPTIONS[RandomGenerator.nextInt(CHAIN_OPTIONS.length)]);
      chain = chainTemp.toString();
    }
  }

  @Override
  public PostfixVariable clone() {
    PostfixVariable clone = new PostfixVariable(this.value.clone(), this.maxChainLength);
    clone.chain = this.chain;
    return clone;
  }

  @Override
  public String toString() {
    return value.id + chain;
  }

  @Override
  public boolean equals(Object obj) {
    PostfixVariable right = (PostfixVariable) obj;
    if (value.id == right.value.id) {
      return true;
    }
    return false;

  }

  private int poisson(double mean) {
    // see: http://rkb.home.cern.ch/rkb/AN16pp/node208.html
    double stop = Math.exp(-mean);
    double prod = 1.0;
    prod *= RandomGenerator.nextDouble();
    int k = 1;
    while (prod >= stop) {
      prod *= RandomGenerator.nextDouble();
      k++;
    }
    return (k - 1);
  }
}
