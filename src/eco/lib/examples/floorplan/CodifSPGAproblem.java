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
package eco.lib.examples.floorplan;

import java.util.ArrayList;
import java.util.Collections;

import eco.core.problem.Problem;
import eco.core.problem.Solution;
import eco.core.problem.Solutions;
import eco.core.problem.Variable;


/**
 *
 * @author cia
 */
public class CodifSPGAproblem extends Problem<Variable<Integer>> {

  protected static FloorplanConfiguration cfgOK;
  protected static FloorplanConfiguration cfgTest;
  protected int layer;
  protected ArrayList<Integer> idNeg;
  protected ArrayList<Integer> idPos;

  //-----------------------------------------------------------Constructor 
  public CodifSPGAproblem(FloorplanConfiguration cfgArgOK,
          FloorplanConfiguration cfgArgTest,
          int layer,
          ArrayList<Integer> idNeg,
          ArrayList<Integer> idPos) {
    //La clase 'padre' debe ser inicializada con:
    // - un nombre: CSP = Cromosoma Sequence Pair
    // - el tamaño del cromosoma
    // - el 3er parametro (=1) es el num.de objetivos
    super(idNeg.size() + idPos.size(), 1);
    /* These are going to be temporary genotypes:
     *  I should go through 'cfgTest' or 'cfgOK' searching for those 
     *   components that are in the same layer than 'layer'
     *  Each match should add the component's id to the arraylist 'ids'
     */
    //Después ya podemos inicializar los miembros de esta clase
    cfgOK = cfgArgOK;
    cfgTest = cfgArgTest;
    this.layer = layer;
    this.idNeg = idNeg; //for 'newRandomSetOfSolutions'
    this.idPos = idPos; //for 'newRandomSetOfSolutions'
  }

  //------------------------------------------------------Metodos necesarios 
  @Override
  public void evaluate(Solutions<Variable<Integer>> solutions) {
    for (Solution<Variable<Integer>> solution : solutions) {
      this.evaluate(solution);
    }
  }

  @Override
  public void evaluate(Solution<Variable<Integer>> solution) {
    /*>1. Transform genotype (solution) into phenotype (cfgTest)
     *    -- solution is an ArrayList<Integer>
     *    -- cfgTest is an instance of FloorplanSP which contains
     *       (cfg) is an instance of FloorplanConfiguration
     *       (layerSP)is an ArrayList<SequencePair> 
     * 
     * [SOLUTION] 
     * Each integer corresponds to the id. of a component _in the layer_ 
     * But a SP codification requires 2 sequences: gammaN & gammaP
     * Thus, solution = gammaN + gammaP , where '+' is 'concatenation'
     * If there are M components in the layer, solution has 2M integers.
     * [SPTEST]
     * 'Sequence Pair Test' is the SP codification out of [SOLUTION]
     * spTest memebers are:
     * ·{gammaP and gammaN sequences = ArrayList of Components}
     * ·{horizontal and vertical graphs}
     * ·a floorplan configuration 
     *  !!To the purpose of this problem spTest should be constructued with 
     *    a given cfg + a given SP codification of a certain layer !!
     */

    SequencePair spTest = new SequencePair(cfgTest, layer);

    //1) Convert solution=ArrayList<Integers> into 2 ArrayList<Components>
    ArrayList<Component> gammaN = new ArrayList<>();
    ArrayList<Component> gammaP = new ArrayList<>();

    int lengthGenotype = solution.getVariables().size();
    for (int k = 0; k <= lengthGenotype / 2 - 1; k++) {
      int id = solution.getVariables().get(k).getValue();
      gammaN.add(spTest.getConfig().components.get(id));
    }
    for (int k = lengthGenotype / 2; k <= lengthGenotype - 1; k++) {
      int id = solution.getVariables().get(k).getValue();
      gammaP.add(spTest.getConfig().components.get(id));
    }
    spTest.setSP(gammaN, gammaP);

    //2) Calculate fitness
    double fitness = 0;
    for (int k = 0; k < solution.getVariables().size() / 2; k++) {
      int idComp = solution.getVariables().get(k).getValue();
      Component cOK = cfgOK.components.get(idComp);
      Component cTest = cfgTest.components.get(idComp);
      //fitness = squared difference of each component with respect to
      //   the given configuration one wants to achieve. 
      fitness += Math.pow((cOK.x - cTest.x), 2)
              + Math.pow((cOK.y - cTest.y), 2)
              + Math.pow((cOK.z - cTest.z), 2);
    }
    solution.getObjectives().set(0, fitness);
  }

  @Override
  public Solutions<Variable<Integer>> newRandomSetOfSolutions(int size) {
    /* [solutions] is an array of 'solution' that represents
     *   the whole population of a generation
     */

    /* Now idNeg and idPos must be shuffled and transformed into 'solution'
     * Then 'solution' is added to 'solutions'
     * And again until 'solutions' has been completed.
     */
    Solutions<Variable<Integer>> solutions = new Solutions<>();
    for (int i = 0; i < size; ++i) {
      Solution<Variable<Integer>> solution = new Solution<>(super.numberOfObjectives);
      Collections.shuffle(idNeg); //<-shuffle the negative seq.
      Collections.shuffle(idPos); //<-shuffle the positive seq.
      //ids=[idNeg idPos]
      ArrayList<Integer> ids = new ArrayList<>();
      for (int k = 0; k < idNeg.size(); k++) {
        ids.add(idNeg.get(k));
      }
      for (int k = 0; k < idPos.size(); k++) {
        ids.add(idPos.get(k));
      }
      //transforming 'ids' into 'solution' element by element
      for (int j = 0; j < numberOfVariables; ++j) {
        solution.getVariables().add(new Variable<>(ids.get(j)));
      }
      solutions.add(solution);
    }
    return solutions;
  }

  @Override
  public Problem<Variable<Integer>> clone() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  /*=================================================================== MAIN ==== 
   //esto es sólo para comprobar el 'evaluate'
   public static void main(String[] args) {
   String nombre_archivo = "ejemplo_2layers.xml"; //<-- poner aqui nombre!
   FloorplanConfiguration cfgOK = new FloorplanConfiguration(
   "profiles" + File.separator + nombre_archivo);
   FloorplanConfiguration cfgTest = new FloorplanConfiguration(
   "profiles" + File.separator + nombre_archivo);        
       
   int capa=0;
   CodifSPGAproblem problema = new CodifSPGAproblem(cfgOK, cfgTest, capa);
   Solutions<Variable<Integer>> poblacion = new Solutions<Variable<Integer>>();
   problema.newRandomSetOfSolutions(poblacion);
        
   /*        
   Solution<Variable<Integer>> solucion = new Solution<Variable<Integer>>();               
   int[] genotipo = new int[cfgOK.components.size()*2];
   for (int k=0; k<cfgOK.components.size(); k++){
   genotipo[k]=cfgOK.components.get(k+1).id; 
   }
   for (int k=0; k<cfgOK.components.size(); k++){
   genotipo[k+cfgOK.components.size()] = cfgOK.components.get(k+1).id;
   }     
   for (int i=0; i<genotipo.length;i++)
   solucion.getVariables().add(new Variable<Integer>(genotipo[i]));
   problema.evaluate(solucion);
   * 
   */
}
