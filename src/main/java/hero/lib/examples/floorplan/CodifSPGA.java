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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import hero.core.algorithm.metaheuristic.moga.NSGAII;
import hero.core.operator.mutation.SwapMutationDouble;
import hero.core.problem.Solution;
import hero.core.problem.Solutions;
import hero.core.problem.Variable;
import hero.lib.examples.floorplan.util.CycleCrossoverDouble;
import hero.lib.examples.floorplan.util.EliteSelectorOperator;

/**
 *
 * @author cia
 */
public class CodifSPGA {
  
    //public static SequencePair run (FloorplanConfiguration cfgOK) {
    public static void run (FloorplanConfiguration cfgOK) {
        
      FloorplanConfiguration cfgTest = cfgOK.clone();
      for (int klayer=0; klayer<cfgOK.numLayers; klayer++){
      /* For EACH layer:  (klayer is the layer counter)
       *   1: find out the components in the k-layer
       *   2: find the SP that best suits the floorplan of the k-layer
       */
        /*(1)*/
        ArrayList<Integer> idNeg = new ArrayList<>();
        ArrayList<Integer> idPos = new ArrayList<>();
        /* These two are going to be temporary genotypes:
         *  I should go through 'cfgTest' (or 'cfgOK') searching for those 
         *   components that are in the same layer than 'layer'
         *  Each match should add the component's id to the arraylist 'ids'
         */
        Iterator<Component> iter = cfgOK.components.values().iterator();
        while (iter.hasNext()) {
            Component currentComponent = iter.next();
            if (currentComponent.z==klayer){
                idNeg.add(currentComponent.id);
                idPos.add(currentComponent.id);
            }
        }
        /*(2)*/
        //SequencePair sp = new SequencePair(cfgTest,klayer);
        //-------------------------------Parameters of the GA
        CodifSPGAproblem probSPGA = new CodifSPGAproblem
                (cfgOK,cfgTest,klayer,idNeg,idPos);
        Integer population = 60;
        Integer generations = 2000;
        double mutationProb = 0.8;
        double crossoverProb = 0.1;
        int elites = population;
        //==================================== run the GA !
        //0. Declare Instance of NSGAII
        NSGAII<Variable<Integer>> nsga2 = new NSGAII<Variable<Integer>>(
                probSPGA,   //<-problem given
                population, //<-num of individuals in a generation
                generations,//<-num of generations
                //BESIDES: Genetic operators must be selected
                new SwapMutationDouble<>(mutationProb),
                //new CycleCrossover<Variable<Integer>>(crossoverProb),
                new CycleCrossoverDouble<Variable<Integer>>(crossoverProb,idNeg.size()),
                new EliteSelectorOperator<Variable<Integer>>(elites) );
                
        //1. Initialize instance 
        nsga2.initialize();
        //2. Execute instance (recall that it is an instance of an algorithm!)
        Solutions<Variable<Integer>> solutions = nsga2.execute();
            //--Logger 
            Logger.getLogger(CodifSPGA.class.getName()).info("solutions.size()=" + solutions.size());
            /*Logger.getLogger(ParallelizeJava.class.getName()).info(solutions.toString());*/
        //3. Once finished, extract the solution = 
        //      First one out of those returned in the execution
        Solution<Variable<Integer>> solution = solutions.get(0);
            //--Logger
            Logger.getLogger(CodifSPGA.class.getName()).info(solution.toString());
        //==================================== GA finished.
        // BUT the outcome is an array of integers = [GammaP,GammaN]
        // NEXT: Obtain GammaP and GammaN out of 'solution'
        // ¿ FALTA => Trasformar solution a sp !!! <= FALTA!?
      printSolutionOfLayer(klayer, solution);      
      }
      //return sp;        
      printConfiguration(cfgTest);
    }
    
    private static void printConfiguration(FloorplanConfiguration cfg){
        /*
        for (int k=1; k<=cfg.components.size();k++){
            System.out.print(
                    cfg.components.get(k).id + ":" + 
                    "(" + 
                    cfg.components.get(k).x + "," + 
                    cfg.components.get(k).y + "," + 
                    cfg.components.get(k).z +  
                    ")" + "[" +
                    cfg.components.get(k).l + "," + 
                    cfg.components.get(k).w + "]" + "\n");
        }*/
        Board board = new Board();
        board.cfg = cfg;
        board.zoom = 7;
        board.buildBoard();
    }
    
    private static void printSolutionOfLayer(int klayer, 
            Solution<Variable<Integer>> solution){
        
        System.out.print("\n Capa " + klayer);
        System.out.print("\n Gamma- = ");
        int offset = solution.getVariables().size()/2;
        for (int k=0; k<offset; k++){
            System.out.print(solution.getVariables().get(k).getValue());
            System.out.print(",");
        }
        System.out.print("\n Gamma+ = ");
        for (int k=offset; k<2*offset; k++){
            System.out.print(solution.getVariables().get(k).getValue());
            System.out.print(",");
        }    
        System.out.print("\n Fitness = ");
            System.out.print(solution.getObjectives().get(0));
            System.out.print("\n--------------------------------------\n");
    }
    
    public static void main(String[] args) {
        //String nombre_archivo = "ejemplo_1layers.xml"; //<-- poner aqui nombre!
        String nombre_archivo = "NHC16L2.xml"; //<-- poner aqui nombre!
        FloorplanConfiguration cfg = new FloorplanConfiguration(
                "profiles" + File.separator + nombre_archivo);
        CodifSPGA.run(cfg);
    }
    
}
