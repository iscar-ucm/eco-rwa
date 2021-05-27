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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import eco.core.algorithm.metaheuristic.moga.NSGAII;
import eco.core.operator.comparator.SolutionDominance;
import eco.core.operator.crossover.CycleCrossover;
import eco.core.operator.selection.BinaryTournamentNSGAII;
import eco.core.problem.Problem;
import eco.core.problem.Solution;
import eco.core.problem.Solutions;
import eco.core.util.logger.HeroLogger;
import eco.core.util.random.RandomGenerator;
import eco.lib.examples.floorplan.util.BinaryNode;

public class FloorplanPolishAsoc extends Problem<PostfixVariable> {

  private static final Logger logger = Logger.getLogger(FloorplanPolishAsoc.class.getName());

  protected static enum BENCHMARK_TYPE {

    N3H48, N3H64, N3H128
  };

  protected static enum OPTIMIZATION_TYPE {

    TEMP, WIRE, WEIGHTED_SUM, MULTI_OBJECTIVE
  };
  protected BENCHMARK_TYPE benchmarkType;
  protected OPTIMIZATION_TYPE optimizationType;
  protected Set<Component> operators = new HashSet<Component>();
  protected FloorplanConfiguration cfg;
  protected boolean useDefaultDesign = true; // Tells if we set the first random individual equal to the original benchmark design
  protected double wireBase = 0.0;
  protected double tempBase = 0.0;

  public FloorplanPolishAsoc(FloorplanConfiguration cfg, BENCHMARK_TYPE benchmarkType, boolean useDefaultDesign, OPTIMIZATION_TYPE optimizationType, int numberOfObjectives) {
    super(cfg.components.size(), numberOfObjectives);
    this.cfg = cfg;
    this.benchmarkType = benchmarkType;
    this.useDefaultDesign = useDefaultDesign;
    this.optimizationType = optimizationType;
    operators.add(new Component(-1, "H"));
    operators.add(new Component(-1, "V"));
    operators.add(new Component(-1, "Z"));
    wireBase = cfg.computeWireObj();
    tempBase = cfg.computeTempObj()[0];
  }

  @Override
  public Solutions<PostfixVariable> newRandomSetOfSolutions(int size) {
    Solutions<PostfixVariable> solutions = new Solutions<PostfixVariable>();
    int firstIndex = 0;
    if (useDefaultDesign) {
      // First solution is the default benchmark
      firstIndex = 1;
      solutions.add(this.buildSolutionForBenchmark(benchmarkType));
    }
    LinkedList<Component> componentsAsList = new LinkedList<Component>();
    componentsAsList.addAll(cfg.components.values());    
    for (int i = firstIndex; i < size; ++i) {
      Solution<PostfixVariable> solution = new Solution<PostfixVariable>(super.numberOfObjectives);
      int j = 0;
      Collections.shuffle(componentsAsList);
      for (Component component : componentsAsList) {
        solution.getVariables().add(new PostfixVariable(component.clone(), j));
        j++;
      }
      solutions.add(solution);
    }
    return solutions;
  }
  
  @Override
  public void evaluate(Solutions<PostfixVariable> solutions) {
    for(Solution<PostfixVariable> solution : solutions) {
      evaluate(solution);
    }
  }

  public void evaluate(Solution<PostfixVariable> solution) {
    int currentLength = 0;
    int currentWidth = 0;
    int currentHeight = 0;
    double dist = 0.0;
    LinkedList<Component> stack = this.buildStackFromSolution(solution);
    BinaryNode<Component> root = this.buildTreeFromStack(stack);
    LinkedList<BinaryNode<Component>> floorplans = this.splicer(root);
    if (root != null && !root.getValue().name.equals("Z")) {
      floorplans.addFirst(root);
    }
    for (BinaryNode<Component> floorplan : floorplans) {
      this.allocate(floorplan, currentHeight++);
      if (floorplan.getValue().l > currentLength) {
        currentLength = floorplan.getValue().l;
      }
      if (floorplan.getValue().w > currentWidth) {
        currentWidth = floorplan.getValue().w;
      }
    }

    double wireObj = 0;
    double tempObj = 0;
    ArrayList<PostfixVariable> variables = solution.getVariables();
    for (int i = 0; i < variables.size(); ++i) {
      Component cI = variables.get(i).getValue();
      if (cI.id < 0) // Es un operador
      {
        continue;
      }
      for (int j = 0; j < variables.size(); ++j) {
        Component cJ = variables.get(j).getValue();
        if (cJ.id > cI.id) {
          // Calculamos el cableado:
          HashSet<Integer> idsTo = cfg.couplings.get(cI.id);
          if (idsTo != null && idsTo.contains(cJ.id)) {
            wireObj += Math.abs(cI.x + cI.l / 2 - cJ.x - cJ.l / 2) + Math.abs(cI.y + cI.w / 2 - cJ.y - cJ.w / 2) + Math.abs(cI.z - cJ.z);
          }
          // Ahora Calculamos el impacto térmico:
          dist = (Math.sqrt(Math.pow(cI.x + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(cI.y + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(cI.z + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2)));
          tempObj += ((cI.dps[0] / cfg.maxDP) * (cJ.dps[0] / cfg.maxDP)) / dist;
        }
      }
    }

    double weightLength = (currentLength > cfg.maxLengthInCells) ? (currentLength - cfg.maxLengthInCells) : 0.0;
    double weightWidth = (currentWidth > cfg.maxWidthInCells) ? (currentWidth - cfg.maxWidthInCells) : 0.0;
    double weightHeight = (currentHeight > cfg.numLayers) ? (currentHeight - cfg.numLayers) : 0.0;
    double unfeasible = weightLength + weightWidth + weightHeight;

    solution.getObjectives().set(0, unfeasible);
    if (optimizationType.equals(OPTIMIZATION_TYPE.TEMP)) {
      solution.getObjectives().set(1, (unfeasible + 1.0) * tempObj);
    } else if (optimizationType.equals(OPTIMIZATION_TYPE.WIRE)) {
      solution.getObjectives().set(1, (unfeasible + 1.0) * wireObj);
    } else if (optimizationType.equals(OPTIMIZATION_TYPE.WEIGHTED_SUM)) {
      solution.getObjectives().set(1, (unfeasible + 1.0) * (wireObj / wireBase + tempObj / tempBase));
    } else if (optimizationType.equals(OPTIMIZATION_TYPE.MULTI_OBJECTIVE)) {
      solution.getObjectives().set(1, (unfeasible + 1.0) * wireObj);
      solution.getObjectives().set(2, (unfeasible + 1.0) * tempObj);
    }
  }

  public Solution<PostfixVariable> buildSolutionFromPostOrder(String postOrder) {
    Solution<PostfixVariable> solution = new Solution<PostfixVariable>(super.getNumberOfObjectives());
    String[] parts = postOrder.split(" ");

    StringBuilder chain = new StringBuilder();
    PostfixVariable var = null;
    for (int i = 0; i < parts.length; ++i) {
      boolean isChainOption = false;
      for (int k = 0; k < PostfixVariable.CHAIN_OPTIONS.length && !isChainOption; ++k) {
        if (parts[i].equals(PostfixVariable.CHAIN_OPTIONS[k])) {
          isChainOption = true;
        }
      }
      if (isChainOption) {
        chain.append(parts[i]);
      } else { // Comienza un nuevo id (nueva variable). Actualizamos la variable anterior y creamos una nueva
        if (var != null) {
          var.chain = chain.toString();
          solution.getVariables().add(var);
        }
        chain = new StringBuilder();
        var = new PostfixVariable(new Component(0, "0"), PostfixVariable.DEFAULT_MAX_CHAIN_LENGTH);
        var.setValue(cfg.components.get(Integer.valueOf(parts[i])).clone());
      }
    }
    if (var != null && chain.length() > 0) { // Hemos leído operadores pero no los hemos asignado a la última variable
      var.chain = chain.toString();
      solution.getVariables().add(var);
    }
    return solution;
  }

  public BinaryNode<Component> buildTreeFromStack(LinkedList<Component> stackRest) {
    if (stackRest.isEmpty()) {
      return null;
    }
    Component currentValue = stackRest.removeLast();
    BinaryNode<Component> node = new BinaryNode<Component>(currentValue);
    if (node.isOperator(operators)) {
      node.setRight(buildTreeFromStack(stackRest));
      node.setLeft(buildTreeFromStack(stackRest));
    }
    return node;
  }

  public LinkedList<Component> buildStackFromSolution(Solution<PostfixVariable> solution) {
    LinkedList<Component> stack = new LinkedList<Component>();
    PostfixVariable var = null;
    int currentLength = 0;
    int currentMaxLength = 0;
    for (int j = 0; j < solution.getVariables().size(); ++j) {
      currentMaxLength = j;
      var = solution.getVariables().get(j);
      stack.add(var.getValue());
      if (currentLength < currentMaxLength) {
        currentLength += var.chain.length();
        if (currentLength > currentMaxLength) {
          currentLength -= var.chain.length();
          var.resizeChain(currentMaxLength - currentLength);
          currentLength = currentMaxLength;
        }
        for (int k = 0; k < var.chain.length(); ++k) {
          stack.add(new Component(-1, var.chain.substring(k, k + 1)));
        }
      }
    }

    while (currentLength < currentMaxLength) {
      stack.add(new Component(-1, PostfixVariable.CHAIN_OPTIONS[RandomGenerator.nextInt(PostfixVariable.CHAIN_OPTIONS.length)]));
      currentLength++;
    }
    return stack;
  }

  public LinkedList<BinaryNode<Component>> splicer(BinaryNode<Component> root) {
    LinkedList<BinaryNode<Component>> trees = new LinkedList<BinaryNode<Component>>();
    if (root.getLeft() != null) {
      trees.addAll(splicer(root.getLeft()));
    }
    if (root.getRight() != null) {
      trees.addAll(splicer(root.getRight()));
    }
    if (root.getValue().name.equals("Z")) {
      BinaryNode<Component> leftChild = root.getLeft();
      BinaryNode<Component> rightChild = root.getRight();
      BinaryNode<Component> parent = root.getParent();
      root.setLeft(null);
      root.setRight(null);
      if (leftChild != null) {
        trees.addFirst(leftChild);
      }
      if (parent != null) {
        if (parent.getLeft() == root) {
          parent.setLeft(rightChild);
        } else if (parent.getRight() == root) {
          parent.setRight(rightChild);
        }
      } else {
        trees.addFirst(rightChild);
      }
    }
    return trees;
  }

  private void allocateFirstStep(BinaryNode<Component> root, Integer layer) {
    Component rootAsBlock = root.getValue();
    rootAsBlock.z = layer;

    if (root.isOperand(operators)) {
      return;
    }

    BinaryNode<Component> fLeft = root.getLeft();
    BinaryNode<Component> fRight = root.getRight();
    allocateFirstStep(fLeft, layer);
    allocateFirstStep(fRight, layer);

    Component leftAsBlock = fLeft.getValue();
    Component rightAsBlock = fRight.getValue();

    if (rootAsBlock.toString().equals("H")) {
      rootAsBlock.l = Math.max(leftAsBlock.l, rightAsBlock.l);
      rootAsBlock.w = leftAsBlock.w + rightAsBlock.w;
    }
    if (rootAsBlock.toString().equals("V")) {
      rootAsBlock.l = leftAsBlock.l + rightAsBlock.l;
      rootAsBlock.w = Math.max(leftAsBlock.w, rightAsBlock.w);
    }

  }

  private void allocateSecondStep(BinaryNode<Component> root, Integer x, Integer y) {
    Component rootAsBlock = root.getValue();
    rootAsBlock.x = x;
    rootAsBlock.y = y;

    if (root.isOperand(operators)) {
      return;
    }

    BinaryNode<Component> fLeft = root.getLeft();
    BinaryNode<Component> fRight = root.getRight();
    allocateSecondStep(fLeft, x, y);

    Component leftAsBlock = fLeft.getValue();
    Integer rightX = null, rightY = null;
    if (rootAsBlock.toString().equals("H")) {
      rightX = leftAsBlock.x;
      rightY = leftAsBlock.y + leftAsBlock.w;
    }
    if (rootAsBlock.toString().equals("V")) {
      rightX = leftAsBlock.x + leftAsBlock.l;
      rightY = leftAsBlock.y;
    }

    allocateSecondStep(fRight, rightX, rightY);
  }

  public void allocate(BinaryNode<Component> floorplan, Integer layer) {
    allocateFirstStep(floorplan, layer);
    allocateSecondStep(floorplan, 0, 0);
  }

  public static void main(String[] args) {
    if (args.length != 5) {
      System.out.println("Usage: java -jar FloorplanPolishAsoc.jar <XmlFilePath> <N3H48|N3H64|N3H128> <true|false> <TEMP|WIRE|WEIGHTED_SUM|MULTI_OBJECTIVE> <TimeInSeconds>");
      System.out.println("Where:");
      System.out.println("<XmlFilePath>: Scenario path file (Absolute or relative)");
      System.out.println("<N3H48|N3H64|N3H128>: Benchmark being optimized");
      System.out.println("<true|false>: Use default design as seed, set this design as one individual of the first random population");
      System.out.println("<TEMP|WIRE|WEIGHTED_SUM|MULTI_OBJECTIVE>: Mono-objective (temp, wire, and weighted sum) or multi-objective (temp and wire)");
      System.out.println("<TimeInSeconds>: Execution time in seconds");
      args = new String[5];
      args[0] = "D:\\jlrisco\\TrabajoExtra\\2012_AppliedSoftComputing\\Results\\N3HC128.xml";
      args[1] = "N3H128";
      args[2] = "true";
      args[3] = "MULTI_OBJECTIVE";
      args[4] = "100";
      return;
    }
    HeroLogger.setup(Level.INFO);
    String xmlFilePath = args[0];
    BENCHMARK_TYPE benchmarkType = BENCHMARK_TYPE.valueOf(args[1]);
    Boolean useDefaultDesign = Boolean.valueOf(args[2]);
    OPTIMIZATION_TYPE optimizationType = OPTIMIZATION_TYPE.valueOf(args[3]);
    Long timeInMiliSeconds = 1000 * Long.valueOf(args[4]);

    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(new File(xmlFilePath.replaceAll(".xml", "_" + optimizationType.toString() + ".txt"))));
    } catch (IOException ex) {
      Logger.getLogger(FloorplanPolishAsoc.class.getName()).log(Level.SEVERE, null, ex);
    }

    Integer numIndi = 100;
    Integer numGene = Integer.MAX_VALUE;
    Integer numberOfObjectives = 2; // UNFEASIBLE objective + one more (temp, wire or weighted sum)
    if (optimizationType.equals(OPTIMIZATION_TYPE.MULTI_OBJECTIVE)) {
      numberOfObjectives = 3;
    }

    FloorplanConfiguration cfg = new FloorplanConfiguration(xmlFilePath);
    FloorplanPolishAsoc problem = new FloorplanPolishAsoc(cfg, benchmarkType, useDefaultDesign, optimizationType, numberOfObjectives);
    NSGAII<PostfixVariable> algorithm = new NSGAII<PostfixVariable>(problem, numIndi, numGene, new PostfixVariable.PostfixMutation(1.0 / problem.getNumberOfVariables()), new CycleCrossover<PostfixVariable>(), new BinaryTournamentNSGAII<PostfixVariable>());

    logger.info("Initializing ...");
    algorithm.initialize();
    logger.info("Running ...");
    long startTime = System.currentTimeMillis();
    long endTime = startTime;

    Solutions<PostfixVariable> solutions = null;
    Solution<PostfixVariable> bestSol = null;
    SolutionDominance<PostfixVariable> comparator = new SolutionDominance<PostfixVariable>();
    boolean updatedBestSol = false;

    StringBuilder line = new StringBuilder();

    while (endTime - startTime < timeInMiliSeconds) {
      algorithm.step();
      endTime = System.currentTimeMillis();
      updatedBestSol = false;

      solutions = algorithm.getPopulation();

      line = new StringBuilder();
      for (Solution<PostfixVariable> solution : solutions) {
        if(comparator.compare(solution, bestSol)<0) {
          bestSol = solution;
          updatedBestSol = true;
        }
      }
      if (updatedBestSol) {
        line.append((endTime - startTime) / 1000.0);
        for(int i=0; i<bestSol.getObjectives().size(); ++i) {
          line.append(" ").append(bestSol.getObjectives().get(i));
        }
        logger.info(line.toString());
        line.append("\n");
        try {
          writer.write(line.toString());
          writer.flush();
        } catch (IOException ex) {
          Logger.getLogger(FloorplanGeneticAsocTemp.class.getName()).log(Level.SEVERE, null, ex);
        }
      }


    }
    logger.info("Done.");

    try {
      writer.close();
      solutions = algorithm.getCurrentSolution();
      problem.save(solutions, xmlFilePath);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    /*JecoLogger.setup(Level.INFO);
    try {
    benchmarkN3End("D:\\jlrisco\\TrabajoExtra\\Dropbox\\Temporal\\2012_ASOC\\N3HC48.xml");
    } catch (IOException ex) {
    Logger.getLogger(FloorplanPolishAsoc.class.getName()).log(Level.SEVERE, null, ex);
    }*/
    /*if (args.length != 4) {
    System.out.println("Parameters: XmlFilePath NumIndividuals NumGenerations SaveResults(0/1)");
    return;
    }*/
    /*String filePath = "D:\\jlrisco\\TrabajoExtra\\Dropbox\\Temporal\\2012_ASOC\\N3HC48.xml";
    Integer numIndi = 100;
    Integer numGene = 20000;
    Integer saveRes = 1;
    FloorplanConfiguration data = new FloorplanConfiguration(filePath);
    FloorplanPolish problem = new FloorplanPolish("TempAware", data);
    Solutions<PostfixVariable> solutions = problem.optimize(numIndi, numGene);
    logger.info(solutions.toString());
    if (saveRes > 0) {
    try {
    problem.save(solutions, filePath);
    } catch (IOException ex) {
    Logger.getLogger(FloorplanPolish.class.getName()).log(Level.SEVERE, null, ex);
    }
    }*/

  }

  public void save(Solutions<PostfixVariable> solutions, String xmlFilePath) throws IOException {
    for (Solution<PostfixVariable> solution : solutions) {
      ArrayList<PostfixVariable> variables = solution.getVariables();
      for (int i = 0; i < variables.size(); ++i) {
        Component c = variables.get(i).getValue();
        if (c.id < 0) // Es un operador
        {
          continue;
        }
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
      String newXmlFilePathSuffix = "_" + FloorplanPolishAsoc.class.getSimpleName() + "_" + this.optimizationType.toString();
      for (int i = 0; i < solution.getObjectives().size(); ++i) {
        newXmlFilePathSuffix += "_" + solution.getObjectives().get(i);
      }
      newXmlFilePathSuffix += ".xml";
      cfg.xmlFilePath = xmlFilePath.replaceAll(".xml", newXmlFilePathSuffix);
      cfg.save();
    }
  }

  public Solutions<PostfixVariable> optimize(Integer numIndi, Integer numGene) {
    NSGAII<PostfixVariable> moga = new NSGAII<PostfixVariable>(this, numIndi, numGene, new PostfixVariable.PostfixMutation(1.0 / this.getNumberOfVariables()), new CycleCrossover<PostfixVariable>(), new BinaryTournamentNSGAII<PostfixVariable>());
    moga.initialize();
    Solutions<PostfixVariable> solutions = moga.execute();
    return solutions;
  }

  /**
   * Benchmarks comming from the ASOC 2012 journal paper. They reproduce the Niagara3 architecture for
   * 48, 64 and 128 cores.
   * @param problem FloorplanPolish configured with the corresponding XML file
   * @param idx 0 for 48 cores, 1 for 64 cores and 2 for 128 cores
   * @return 
   */
  public Solution<PostfixVariable> buildSolutionForBenchmark(BENCHMARK_TYPE benchmarkType) {
    // Polish notation for N3H48, one array component per layer, index 0 is for layer 0, index 1 for layer 1, etc.
    String[] benchmarkN3H48 = {"1 5 13 H V 6 14 H V 7 15 H V 8 16 H V 4 V 21 H 2 17 9 H V 18 10 H V 19 11 H V 20 12 H V 3 V H",
      "22 26 34 H V 27 35 H V 28 36 H V 29 37 H V 25 V 42 H 23 38 30 H V 39 31 H V 40 32 H V 41 33 H V 24 V H",
      "83 99 H 84 100 H V 115 H 85 101 H 86 102 H V 116 H V 87 103 H 88 104 H V 117 H V 89 105 H 90 106 H V 118 H V 123 H 119 107 91 H 108 92 H V H 120 109 93 H 110 94 H V H V 121 111 95 H 112 96 H V H V 122 113 97 H 114 98 H V H V H",
      "124 140 H 125 141 H V 156 H 126 142 H 127 143 H V 157 H V 128 144 H 129 145 H V 158 H V 130 146 H 131 147 H V 159 H V 164 H 160 148 132 H 149 133 H V H 161 150 134 H 151 135 H V H V 162 152 136 H 153 137 H V H V 163 154 138 H 155 139 H V H V H"};
    String[] benchmarkN3H64 = {"1 5 13 H V 6 14 H V 7 15 H V 8 16 H V 4 V 21 H 2 17 9 H V 18 10 H V 19 11 H V 20 12 H V 3 V H",
      "22 26 34 H V 27 35 H V 28 36 H V 29 37 H V 25 V 42 H 23 38 30 H V 39 31 H V 40 32 H V 41 33 H V 24 V H",
      "83 99 H 84 100 H V 115 H 85 101 H 86 102 H V 116 H V 87 103 H 88 104 H V 117 H V 89 105 H 90 106 H V 118 H V 123 H 119 107 91 H 108 92 H V H 120 109 93 H 110 94 H V H V 121 111 95 H 112 96 H V H V 122 113 97 H 114 98 H V H V H",
      "124 140 H 125 141 H V 156 H 126 142 H 127 143 H V 157 H V 128 144 H 129 145 H V 158 H V 130 146 H 131 147 H V 159 H V 164 H 160 148 132 H 149 133 H V H 161 150 134 H 151 135 H V H V 162 152 136 H 153 137 H V H V 163 154 138 H 155 139 H V H V H",
      "165 181 H 166 182 H V 197 H 167 183 H 168 184 H V 198 H V 169 185 H 170 186 H V 199 H V 171 187 H 172 188 H V 200 H V 205 H 201 189 173 H 190 174 H V H 202 191 175 H 192 176 H V H V 203 193 177 H 194 178 H V H V 204 195 179 H 196 180 H V H V H"};
    String[] benchmarkN3H128 = {"1 5 13 H V 6 14 H V 7 15 H V 8 16 H V 4 V 21 H 2 17 9 H V 18 10 H V 19 11 H V 20 12 H V 3 V H",
      "22 26 34 H V 27 35 H V 28 36 H V 29 37 H V 25 V 41 H 23 38 30 H V 39 31 H V 40 32 H V 33 24 V V H",
      "42 58 H 43 59 H V 74 H 44 60 H 45 61 H V 75 H V 46 62 H 47 63 H V 76 H V 48 64 H 49 65 H V 77 H V 82 H 78 66 50 H 67 51 H V H 79 68 52 H 69 53 H V H V 80 70 54 H 71 55 H V H V 81 72 56 H 73 57 H V H V H",
      "124 140 H 125 141 H V 156 H 126 142 H 127 143 H V 157 H V 128 144 H 129 145 H V 158 H V 130 146 H 131 147 H V 159 H V 164 H 160 148 132 H 149 133 H V H 161 150 134 H 151 135 H V H V 162 152 136 H 153 137 H V H V 163 154 138 H 155 139 H V H V H",
      "165 181 H 166 182 H V 197 H 167 183 H 168 184 H V 198 H V 169 185 H 170 186 H V 199 H V 171 187 H 172 188 H V 200 H V 205 H 201 189 173 H 190 174 H V H 202 191 175 H 192 176 H V H V 203 193 177 H 194 178 H V H V 204 195 179 H 196 180 H V H V H",
      "206 222 H 207 223 H V 238 H 208 224 H 209 225 H V 239 H V 210 226 H 211 227 H V 240 H V 212 228 H 213 229 H V 241 H V 246 H 242 230 214 H 231 215 H V H 243 232 216 H 233 217 H V H V 244 234 218 H 235 219 H V H V 245 236 220 H 237 221 H V H V H",
      "247 263 H 248 264 H V 279 H 249 265 H 250 266 H V 280 H V 251 267 H 252 268 H V 281 H V 253 269 H 254 270 H V 282 H V 287 H 283 271 255 H 272 256 H V H 284 273 257 H 274 258 H V H V 285 275 259 H 276 260 H V H V 286 277 261 H 278 262 H V H V H",
      "288 304 H 289 305 H V 320 H 290 306 H 291 307 H V 321 H V 292 308 H 293 309 H V 322 H V 294 310 H 295 311 H V 323 H V 328 H 324 312 296 H 313 297 H V H 325 314 298 H 315 299 H V H V 326 316 300 H 317 301 H V H V 327 318 302 H 319 303 H V H V H",
      "329 345 H 330 346 H V 361 H 331 347 H 332 348 H V 362 H V 333 349 H 334 350 H V 363 H V 335 351 H 336 352 H V 364 H V 369 H 365 353 337 H 354 338 H V H 366 355 339 H 356 340 H V H V 367 357 341 H 358 342 H V H V 368 359 343 H 360 344 H V H V H"};
    // Set current benchmark
    String[] benchmark = null;
    if (benchmarkType.equals(BENCHMARK_TYPE.N3H48)) {
      benchmark = benchmarkN3H48;
    } else if (benchmarkType.equals(BENCHMARK_TYPE.N3H64)) {
      benchmark = benchmarkN3H64;
    } else if (benchmarkType.equals(BENCHMARK_TYPE.N3H128)) {
      benchmark = benchmarkN3H128;
    }

    // We build one tree per layer, without Z nodes
    ArrayList<BinaryNode<Component>> trees = new ArrayList<BinaryNode<Component>>();
    for (int i = 0; i < benchmark.length; ++i) {
      Solution<PostfixVariable> solution = this.buildSolutionFromPostOrder(benchmark[i]);
      LinkedList<Component> stack = this.buildStackFromSolution(solution);
      BinaryNode<Component> tree = this.buildTreeFromStack(stack);
      trees.add(tree);
    }
    // Starting with the first tree, we look for the first level with enough number of nodes to place 'Z' nodes and subsequent layers
    BinaryNode<Component> firstTree = trees.get(0);
    int currentLevel = 0; // Start with level 0
    int zNodesNeeded = benchmark.length - 1;
    LinkedList<BinaryNode<Component>> nodesAtLevelI = firstTree.getNodesAtLevel(currentLevel);
    while (nodesAtLevelI.size() < zNodesNeeded) {
      currentLevel++;
      nodesAtLevelI = firstTree.getNodesAtLevel(currentLevel);
    }
    // Now we insert "Z" nodes
    for (int i = 0; i < zNodesNeeded; ++i) {
      BinaryNode<Component> oldParent = nodesAtLevelI.remove();
      BinaryNode<Component> newParent = new BinaryNode<Component>(new Component(-1, "Z"));
      newParent.setLeft(trees.get(i + 1));
      if (oldParent.getParent().getLeft() == oldParent) {
        oldParent.getParent().setLeft(newParent);
      } else if (oldParent.getParent().getRight() == oldParent) {
        oldParent.getParent().setRight(newParent);
      }
      newParent.setRight(oldParent);
    }
    // Done :-)
    String firstTreeAsString = firstTree.toString();
    Solution<PostfixVariable> solution = this.buildSolutionFromPostOrder(firstTreeAsString);
    return solution;
  }

  @Override
  public Problem<PostfixVariable> clone() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
