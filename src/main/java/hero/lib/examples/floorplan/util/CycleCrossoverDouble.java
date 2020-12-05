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
package hero.lib.examples.floorplan.util;

import hero.core.operator.crossover.CrossoverOperator;
import hero.core.problem.Solutions;
import hero.core.problem.Solution;
import hero.core.problem.Variable;
import hero.core.util.random.RandomGenerator;
import java.util.ArrayList;

/**
 *
 * @author cia
 * @param <T>
 */
public class CycleCrossoverDouble<T extends Variable<Integer>> extends CrossoverOperator<T> {

    public static final double DEFAULT_PROBABILITY = 0.9;
    protected double probability;
    protected int frontier = 0;

    public CycleCrossoverDouble() {
        probability = DEFAULT_PROBABILITY;
    }

    public CycleCrossoverDouble(double probability, int initFrontier) {
        this.probability = probability;
        this.frontier = initFrontier;
    }

    public Solutions<T> doCrossover(double probability,
            Solution<T> parent1, Solution<T> parent2) {
        /* 
         * (1) recover genotypes gammaN and gammaP 
         *     out of parent 1 and parent2
         */
        ArrayList<Integer> gammaN1 = new ArrayList<>();
        ArrayList<Integer> gammaN2 = new ArrayList<>();
        ArrayList<Integer> gammaP1 = new ArrayList<>();
        ArrayList<Integer> gammaP2 = new ArrayList<>();
        for (int k = 0; k < frontier; k++) {
            gammaN1.add(parent1.getVariables().get(k).getValue());
            gammaN2.add(parent2.getVariables().get(k).getValue());
        }
        for (int k = frontier; k < 2 * frontier; k++) {
            gammaP1.add(parent1.getVariables().get(k).getValue());
            gammaP2.add(parent2.getVariables().get(k).getValue());
        }
        /* 
         * (2) IF probability decides to xover:
         *     cycle xover gammaN1 with gammaP1.
         * SAME for cycle xover with gammaN2 and gammaP2.
         */
        if (RandomGenerator.nextDouble() <= probability) {
            cycleXover(gammaN1, gammaN2);
        }
        if (RandomGenerator.nextDouble() <= probability) {
            cycleXover(gammaP1, gammaP2);
        }
        /*
         * (3) create offspring
         */
        Solutions<T> offSpring = new Solutions<>();
        offSpring.add(parent1.clone());
        offSpring.add(parent2.clone());
        /*
         * (4) set new genotypes into offspring
         */        
        for (int i=0; i<gammaN1.size(); i++){
            int aux0 = gammaN1.get(i);
            int aux1 = gammaN2.get(i);
            offSpring.get(0).getVariables().get(i).setValue( (Integer) aux0 );
            offSpring.get(1).getVariables().get(i).setValue( (Integer) aux1 );
        }
        int offset = gammaN1.size();
        for (int i=0; i<gammaP1.size(); i++){
            int aux0 = gammaP1.get(i);
            int aux1 = gammaP2.get(i);
            offSpring.get(0).getVariables().get(i+offset).setValue( (Integer) aux0 );
            offSpring.get(1).getVariables().get(i+offset).setValue( (Integer) aux1 );
        }        
        return offSpring;
    }

    private void cycleXover(ArrayList<Integer> p1, ArrayList<Integer> p2) {
        //preliminares:
        ArrayList<Integer> fijos = new ArrayList<>();
        //1.elegir un alelo en p1.                                
        int posAT1 = RandomGenerator.nextInt(0, p1.size());
        //2.bucle para localizar los fijos
        int cont = 0;
        boolean terminado = false;
        while (!terminado && cont < p1.size()) {
            cont++;
            fijos.add(posAT1);
            //2.1.mirar valor de p2 en posAT1                                       
            int valor = p2.get(posAT1);
            //2.2.localizar 'valor' en p1.                              
            int posAT2 = p1.indexOf(valor);
            //
            if (posAT2 == fijos.get(0)) {
                terminado = true;
            } else {
                posAT1 = posAT2;
            }
        }
        //3.copiar
        ArrayList<Integer> q1 = new ArrayList<>();
        //q1.addAll(p2); !esto es lo mismo que el for de abajo!
        for (int k = 0; k < p2.size(); k++) {
            q1.add(p2.get(k));
        }
        ArrayList<Integer> q2 = new ArrayList<>();
        for (int k = 0; k < p1.size(); k++) {
            q2.add(p1.get(k));
        }
        
        //4.mantener los fijos
        for (int k = 0; k < fijos.size(); k++) {
            q1.set(fijos.get(k), p1.get(fijos.get(k)));
            q2.set(fijos.get(k), p2.get(fijos.get(k)));
        }
        p1.clear();
        p1.addAll(q1);
        p2.clear();
        p2.addAll(q2); 
    }

    /*
    Solutions<T> offSpring = new Solutions<T>();
    
    offSpring.add(parent1.clone());
    offSpring.add(parent2.clone());
    
    if (RandomGenerator.nextDouble() <= probability) {
    // We obtain the cycle, first allele:
    Integer currentPos = 0;
    LinkedList<Integer> cycle = new LinkedList<Integer>();
    cycle.add(currentPos);
    
    T variable = parent2.getVariables().get(currentPos);
    currentPos = lookForPosition(parent1, variable);
    while (currentPos != 0) {
    cycle.add(currentPos);
    variable = parent2.getVariables().get(currentPos);
    currentPos = lookForPosition(parent1, variable);
    }
    
    Solution<T> clon1 = parent1.clone();
    Solution<T> clon2 = parent2.clone();
    for (int i = 0; i < parent1.getVariables().size(); ++i) {
    if (cycle.contains(i)) {
    offSpring.get(0).getVariables().set(i, clon1.getVariables().get(i));
    offSpring.get(1).getVariables().set(i, clon2.getVariables().get(i));
    } else {
    offSpring.get(0).getVariables().set(i, clon2.getVariables().get(i));
    offSpring.get(1).getVariables().set(i, clon1.getVariables().get(i));
    }
    }
    }
    offSpring.get(0).setEvaluated(false);
    offSpring.get(1).setEvaluated(false);
    return offSpring;
    */
    
    /**
     * Executes the operation
     * @param parent1
     * @param parent2
     * @return An object containing the offSprings
     */
    @Override
    public Solutions<T> execute(Solution<T> parent1, Solution<T> parent2) {
        return doCrossover(probability, parent1, parent2);
    } // execute
    
    /*
    public static void main(String[] args) {
         ArrayList<Integer> p1 = new ArrayList();
         ArrayList<Integer> p2 = new ArrayList();
         this.cycleXover(p1,p2); //<--no me deja :(
     }
     */
    
} // CX

