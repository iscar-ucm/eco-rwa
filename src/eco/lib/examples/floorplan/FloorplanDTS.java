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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import eco.core.algorithm.metaheuristic.moga.NSGAII;
import eco.core.operator.crossover.CycleCrossover;
import eco.core.operator.selection.BinaryTournamentNSGAII;
import eco.lib.examples.floorplan.util.MultiwayTree;
import eco.lib.examples.floorplan.util.Solution;

import java.util.Collection;

/**
 * Class implementing the floorplaning DTS (Double Tree and Sequence) notation.
 * It is a 3D o-tree based notation.
 *
 * @author J. M. Colmenar
 */
public class FloorplanDTS extends FloorplanSolution {

  /**
   * x axis limit (length)
   */
  protected int maxX;
  /**
   * y axis limit (width)
   */
  protected int maxY;
  /**
   * z axis limit (height)
   */
  protected int maxZ;
  /**
   * Current max long x
   */
  protected int currMaxLongX = 0;
  /**
   * Current max width y
   */
  protected int currMaxWidthY = 0;
  /**
   * Current max height Z
   */
  protected int currMaxHeightZ = 0;
  /**
   * Parameter to avoid re-calculate coordinates
   */
  protected boolean coordsComputed = false;
  // Weights for the objectives:
  private static final double COMMON_WEIGHT = 1.0 / 2.0;
  protected double wireWeight = COMMON_WEIGHT;
  protected double temperatureWeight = COMMON_WEIGHT;
  // DTS encoding
  protected MultiwayTree<Component> xTree = new MultiwayTree<>();
  protected MultiwayTree<Component> yTree = new MultiwayTree<>();
  protected ArrayList<Component> zOrder = new ArrayList<>();
  /**
   * Random number generator
   */
  private static Random rnd = new Random();
  /**
   * Flag: if true, each "toString" call will also backup the floorplan to a xml
   * file.
   */
  public static boolean backupToXML = true;
  private static double startTime = System.currentTimeMillis() / 1000;   // In seconds

  /* For output print */
  public static String outputDir = "";
  public static String xmlFileName = "";

  FloorplanDTS() {
    /* Create trees as just root */
    xTree.setRoot(true);
    xTree.setNode(emptyRootNode());
    yTree.setRoot(true);
    yTree.setNode(emptyRootNode());
  }

  /**
   * Detailed constructor
   *
   * @param config floorplan configuration based on components
   * @param volumeWgt weight to be applied to volume in the objective
   * computation
   * @param wireWgt weight to be applied to wiring in the objective computation
   */
  FloorplanDTS(FloorplanConfiguration config, double wireWgt, double tempWgt,
          long seed, boolean randomEncoding, String currOutputDir) {
    this();

    rnd = new Random(seed);

    String[] tokens = config.xmlFilePath.split(File.separator);
    xmlFileName = tokens[tokens.length - 1];
    outputDir = currOutputDir;

    // Process configuration
    cfg = config;
    // Set floorplan limits
    maxX = cfg.maxLengthInCells;
    maxY = cfg.maxWidthInCells;
    maxZ = cfg.numLayers;

    // Set fitness function weights
    wireWeight = wireWgt;
    temperatureWeight = tempWgt;

    if (randomEncoding) {
      randomEncoding();
    }

    // Enconding must be performed even after randomEncoding
    encode();

    // State initial x, y and z positions:
    computeCoordinates();

    // Starting objective values
    startingWiring = computeWiring();
    startingTemp = computeTemperature();

  }

  /**
   * Codification method: from the FloorplanConfiguration of the object, the
   * method re-creates the configuration in current object trees.
   */
  private void encode() {
    if (cfg == null) {
      throw new UnsupportedOperationException("Unexisting floorplan configuration");
    }

    // In order to obtain number of layers, sort by z
    List<Component> elems = new ArrayList<Component>(cfg.components.values());

    // 1.- Sort by z coordinate to obtain zOrder
    Collections.sort(elems, new Comparator<Component>() {
      public int compare(Component o1, Component o2) {
        if (o1.z < o2.z) {
          return -1;
        } else if (o1.z == o2.z) {
          return 0;
        } else {
          return 1;
        }
      }
    });

    // Fill in the zOrder:
    zOrder.addAll(elems);

    // 2.- Sort by x, then by y coordinates to obtain x-tree
    Collections.sort(elems, new Comparator<Component>() {
      public int compare(Component o1, Component o2) {
        if (o1.x < o2.x) {
          return -1;
        } else if (o1.x == o2.x) {
          // Sort by y
          if (o1.y < o2.y) {
            return -1;
          } else if (o1.y == o2.y) {
            return 0;
          } else {
            return 1;
          }
        } else {
          return 1;
        }
      }
    });

    // Fill in x-tree
    Iterator<Component> iter = elems.iterator();
    Component currComp = null;

    // xTree points to the node where components must be inserted
    // xTree starts in root
    while (iter.hasNext()) {
      currComp = iter.next();
      List<Component> currentTreeNodes = xTree.breadthFirstTraversal();
      Component parent = null;
      int currDistance = Integer.MAX_VALUE;
      // Review all components to obtain the nearest in "x" distance
      Iterator<Component> iterTree = currentTreeNodes.iterator();
      while (iterTree.hasNext() && (currDistance != 0)) {
        Component prevComp = iterTree.next();
        int newDistance = currComp.x - (prevComp.x + prevComp.l);
        if ((newDistance >= 0) && (newDistance < currDistance)) {
          currDistance = newDistance;
          parent = prevComp;
        }
      }

      if (parent != null) {
        xTree.findNode(parent).addChildren(currComp);
      } else {
        System.err.println("Floorplan coordinates error: component " + currComp + " couldn't be positioned on xTree !!");
        System.err.println("--> Inserted at top level");
        xTree.addChildren(currComp);

      }

    }


    // 3.- Sort by y, then by x coordinates to obtain y-tree
    Collections.sort(elems, new Comparator<Component>() {
      public int compare(Component o1, Component o2) {
        if (o1.y < o2.y) {
          return -1;
        } else if (o1.y == o2.y) {
          // Sort by x
          if (o1.x < o2.x) {
            return -1;
          } else if (o1.x == o2.x) {
            return 0;
          } else {
            return 1;
          }
        } else {
          return 1;
        }
      }
    });

    // Fill in y-tree
    iter = elems.iterator();
    currComp = null;

    // yTree points to the node where components must be inserted
    // yTree starts in root
    while (iter.hasNext()) {
      currComp = iter.next();
      List<Component> currentTreeNodes = yTree.breadthFirstTraversal();
      Component parent = null;
      int currDistance = Integer.MAX_VALUE;
      // Review all components to obtain the nearest in "y" distance
      Iterator<Component> iterTree = currentTreeNodes.iterator();
      while (iterTree.hasNext() && (currDistance != 0)) {
        Component prevComp = iterTree.next();
        int newDistance = currComp.y - (prevComp.y + prevComp.w);
        if ((newDistance >= 0) && (newDistance < currDistance)) {
          currDistance = newDistance;
          parent = prevComp;
        }
      }

      if (parent != null) {
        yTree.findNode(parent).addChildren(currComp);
      } else {
        System.err.println("Floorplan coordinates error: component " + currComp + " couldn't be positioned on yTree !!");
        System.err.println("--> Inserted at top level");
        yTree.addChildren(currComp);
      }

    }

  }

  /**
   * Fills in cfg with coordinates coming from random. Requires a posterior
   * encoding.
   *
   * Uses MOEA to generate random !!
   */
  private void randomEncoding() {

    // Take the MOEA approach to start from random like it
    FloorplanGeneticAsocMultiObj problem = new FloorplanGeneticAsocMultiObj(cfg);
    NSGAII<ComponentVariable> algorithm = new NSGAII<ComponentVariable>(problem, 1, 0, new ComponentVariable.ComponentMutation(1.0 / problem.getNumberOfVariables()), new CycleCrossover<>(), new BinaryTournamentNSGAII<ComponentVariable>());
    algorithm.initialize();
    // Take one individual from population where coordinates have been established.
    eco.core.problem.Solution<ComponentVariable> indiv = algorithm.getPopulation().get(0);

    // Modify cfg coordinates:
    ArrayList<ComponentVariable> variables = indiv.getVariables();
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

    /*
     *  OLD RANDOM VERSION
     // Random initial solution (different seeds to avoid same tree)
     xTree.randomTree(cfg.getComponents().values());
     yTree.randomTree(cfg.getComponents().values());
     Iterator<Component> it = cfg.getComponents().values().iterator();
     while (it.hasNext()) zOrder.add(it.next());
     Collections.shuffle(zOrder);
     */

  }

  /**
   * Parses the given tree on a deep first basis obtaining the size of the
   * longest branch.
   *
   * At the same time, <b>all nodes are getting filled their x or y values in
   * the component list of the configuration</b>
   *
   * @param tree
   * @param x true means to select length (l), false means width (w)
   * @return size
   */
  private int obtainTreeSize(MultiwayTree<Component> tree, boolean x) {
    int size = 0;
    if (!tree.isRoot()) {
      Component node = cfg.components.get(tree.getNode().id);
      Component parentNode = cfg.components.get(tree.getParent().getNode().id);

      if (parentNode == null) {
        // Parent is root, which is not in the components list. Create new
        parentNode = emptyRootNode();
      }

      if (x) {
        // Length
        size += node.l;
        // Set x in common list !!
        node.x = parentNode.x + parentNode.l;
      } else {
        // Width
        size += node.w;
        // Set y in common list !!
        node.y = parentNode.y + parentNode.w;
      }
    }

    int childrenMaxSize = 0;

    if (tree.getChildren() != null) {
      Iterator<MultiwayTree<Component>> iter = tree.getChildren().iterator();
      while (iter.hasNext()) {
        MultiwayTree<Component> child = iter.next();
        int childSize = obtainTreeSize(child, x);
        if (childSize > childrenMaxSize) {
          childrenMaxSize = childSize;
        }
      }
    }

    return (size + childrenMaxSize);
  }

  /**
   * <b> Fills in the x, y and z values for the components.</b>
   *
   * Also calculates the maximum longitude, width and height of the layers,
   * filling in their values into the corresponding attributes.
   *
   */
  private void computeCoordinates() {

    if (!coordsComputed) {
      // For the trees, it has to find the deepest path in terms of size
      // The x and y coordinates for the components are set here
      currMaxLongX = obtainTreeSize(xTree, true);
      currMaxWidthY = obtainTreeSize(yTree, false);

      /* The number of layers is obtained by checking wich nodes are
       * overlapped in the x-y plane. Then, we have to keep the nodes
       * processed for each layer */
      currMaxHeightZ = 0;
      Iterator<Component> iter = zOrder.iterator();
      ArrayList<Component> layerNodes = new ArrayList<Component>();
      Component currNode = null;
      while (iter.hasNext()) {
        currNode = cfg.components.get(iter.next().id);
        if (!layerNodes.isEmpty()) {
          boolean overlap = false;
          Iterator<Component> iterLayerNodes = layerNodes.iterator();
          // Check overlapping with any of the nodes of this layer
          while (!overlap && iterLayerNodes.hasNext()) {
            Component layerNode = iterLayerNodes.next();
            // Overlap ?
            overlap = overlap(currNode, layerNode);
          }
          if (overlap) {
            currMaxHeightZ += 1;
            layerNodes.clear();
          }

        }
        currNode.z = currMaxHeightZ;
        layerNodes.add(currNode);
      }

      coordsComputed = true;
      cfg.numLayers = currMaxHeightZ + 1;
    }

  }

  @Override
  public boolean isFeasible() {
    boolean feasible = false;

    computeCoordinates();

    // Must check all the dimensions (z must be less because starts in 0):
    if ((currMaxLongX <= maxX) && (currMaxWidthY <= maxY) && (currMaxHeightZ < maxZ)) {
      feasible = true;
    }

    return feasible;
  }

  @Override
  public double getObjective() {
    // The objective is a weighted function:
    computeCoordinates();

    double objective = 0.0;

    if ((wireWeight == 1.0) || (temperatureWeight == 1.0)) {
      objective = ((wireWeight * computeWiring()) + (temperatureWeight * computeTemperature()));
    } else {
      objective = ((wireWeight * computeWiring() / startingWiring) + (temperatureWeight * computeTemperature() / startingTemp));
    }

    if (!isFeasible()) {
      double currVolume = (currMaxLongX * currMaxWidthY * (currMaxHeightZ + 1));
      double maxVolume = (maxX * maxY * maxZ);
      double penalty = currVolume / maxVolume;
      objective *= penalty;
    }

    return objective;
  }

  @Override
  public Enumeration<Solution> getSuccessors() {
    ArrayList<Solution> succesors = new ArrayList<>();

    FloorplanDTS solution = (FloorplanDTS) this.clone();

    /* Neighbor is generated through two different operations:
     * 1.- Change x-tree or y-tree by moving a node
     * 2.- Swap two elements in the x-tree, y-tree or z-order */

    // Move random node
    if (rnd.nextBoolean()) {
      // Change x-tree
      moveRandomNode(solution.xTree);
    } else {
      // Change y-tree
      moveRandomNode(solution.yTree);
    }

    // Swap random elements
    int who = rnd.nextInt(3);
    switch (who) {
      case 0:  // Change x-tree
        swapRandomNode(solution.xTree);
        break;
      case 1:  // Change y-tree
        swapRandomNode(solution.yTree);
        break;
      default: // Change z-order
        swapRandomNode(solution.zOrder);
        break;
    }

    // New volume has to be computed
    solution.coordsComputed = false;
    solution.computeCoordinates();

    Solution sol = solution;
    succesors.add(sol);

    return Collections.enumeration(succesors);
  }

  @Override
  public Solution clone() {
    FloorplanDTS clone = new FloorplanDTS();

    clone.maxX = this.maxX;
    clone.maxY = this.maxY;
    clone.maxZ = this.maxZ;

    clone.xTree = this.xTree.clone();
    clone.yTree = this.yTree.clone();

    ArrayList<Component> clonedZ = new ArrayList<Component>();
    for (int i = 0; i < zOrder.size(); i++) {
      clonedZ.add(this.zOrder.get(i));
    }

    clone.zOrder = clonedZ;

    clone.temperatureWeight = this.temperatureWeight;
    clone.wireWeight = this.wireWeight;

    clone.cfg = this.cfg.clone();
    clone.startingTemp = this.startingTemp;
    clone.startingWiring = this.startingWiring;

    clone.computeCoordinates();

    return clone;
  }

  public int compareTo(Solution rhs) {
    double objLhs = this.getObjective();
    double objRhs = rhs.getObjective();
    if (objLhs < objRhs) {
      return -1;
    }
    if (objLhs == objRhs) {
      return 0;
    }
    return 1;
  }

  /**
   * Decodes the current solution from DTS to our notation *
   */
  FloorplanConfiguration decode() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public String toString() {
    String cad = "";

    // x-tree:
    cad += "x-tree --> " + xTree;
    cad += "\ny-tree --> " + yTree;
    cad += "\nz-order --> ";
    Iterator<Component> iter = zOrder.iterator();
    while (iter.hasNext()) {
      Component cmp = iter.next();
      cad += cmp.toString();
      cad += " (" + cfg.components.get(cmp.id).x + ",";
      cad += cfg.components.get(cmp.id).y + ",";
      cad += cfg.components.get(cmp.id).z + ")  ";
    }
    cad += "\nFeasible: " + isFeasible() + " - Fitness: " + this.getObjective() + " ( Wiring (" + wireWeight + " * " + computeWiring();
    if (wireWeight != 1.0) {
      cad += " / " + startingWiring;
    }
    cad += ")  +  Temperature ( " + temperatureWeight + " * " + computeTemperature();
    if (temperatureWeight != 1.0) {
      cad += " / " + startingTemp;
    }
    cad += ") )";

    if (backupToXML) {
      String newXmlFilePathSuffix = "_" + ((System.currentTimeMillis() / 1000) - startTime) + ".xml";
      String xmlFilePath = xmlFileName.replaceAll(".xml", newXmlFilePathSuffix);
      try {
        save(outputDir, xmlFilePath);
      } catch (IOException ex) {
        Logger.getLogger(FloorplanDTS.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    return cad;
  }

  /**
   * Selects a node by random and inserts it in a random new position
   *
   * @param tree where the operation will be performed
   */
  private void moveRandomNode(MultiwayTree<Component> tree) {

    int indexOfNode = 0;
    Component node = null;

    // The node to be selected is randomly chosen from the components list:
    do {
      indexOfNode = rnd.nextInt(cfg.components.size());
      node = cfg.components.get(indexOfNode);
      // Avoid selecting the root pseudo-node
    } while (node == null);

    tree.removeNode(node, tree);

    // Insert in random position:
    // 1.- Obtain a random number
    indexOfNode = rnd.nextInt(cfg.components.size());
    // 2.- Search for the subtree for the indexOfNode-th node
    MultiwayTree<Component> subTree = tree.getSubTree(indexOfNode, tree);
    // 3.- Insert the extracted node in that position:
    if (indexOfNode == 0) {
      // Would mean to be "brother" of root -> put as i-th child of root
      indexOfNode = rnd.nextInt(subTree.getChildren().size() + 1);
      subTree.addChildren(indexOfNode, node);
    } else {
      // Randomly put on left, right or new child of the node
      int pos = rnd.nextInt(3);
      switch (pos) {
        case 0: // left
          indexOfNode = subTree.getParent().getChildren().indexOf(subTree);
          subTree.getParent().addChildren(indexOfNode, node);
          break;
        case 1: // right
          indexOfNode = subTree.getParent().getChildren().indexOf(subTree) + 1;
          subTree.getParent().addChildren(indexOfNode, node);
          break;
        default: // new child
          subTree.addChildren(node);
      }
    }

  }

  /**
   * Selects two nodes by random and swap them
   *
   * @param tree where the operation will be performed
   */
  private void swapRandomNode(MultiwayTree<Component> tree) {

    int indexOfNode1 = 0;
    int indexOfNode2 = 0;

    // The nodes to be selected are randomly chosen from the components list:
    do {
      indexOfNode1 = rnd.nextInt(cfg.components.size());
      indexOfNode2 = rnd.nextInt(cfg.components.size());
      // Avoid selecting the root pseudo-node
    } while ((indexOfNode1 == 0) || (indexOfNode2 == 0));

    // Obtain the subtrees
    MultiwayTree<Component> subTree1 = tree.getSubTree(indexOfNode1, tree);
    MultiwayTree<Component> subTree2 = tree.getSubTree(indexOfNode2, tree);

    // Swap nodes
    Component aux = subTree1.getNode();
    subTree1.setNode(subTree2.getNode());
    subTree2.setNode(aux);

  }

  /**
   * Selects two nodes by random and swap them
   *
   * @param sequence where the operation will be performed
   */
  private void swapRandomNode(ArrayList<Component> seq) {
    int indexOfNode1 = 0;
    int indexOfNode2 = 0;

    // The nodes to be selected are randomly chosen from the components list:
    indexOfNode1 = rnd.nextInt(cfg.components.size());
    indexOfNode2 = rnd.nextInt(cfg.components.size());

    // Swap nodes
    Component aux = seq.get(indexOfNode1);
    seq.set(indexOfNode1, seq.get(indexOfNode2));
    seq.set(indexOfNode2, aux);

  }

  /**
   * Creates a blank node corresponding to a root node where all elements are 0.
   *
   * @return node
   */
  private Component emptyRootNode() {
    return new Component(0, "root", -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new double[]{0.0});
  }

  /**
   * Returns true if the given components overlap in the x-y plane
   *
   * @param node1
   * @param node2
   *
   * @return
   */
  private boolean overlap(Component node1, Component node2) {

    boolean overlap = false;
    boolean overlapX = false;

    if (((node1.x <= node2.x)
            && ((node1.x + node1.l) > node2.x))
            || ((node1.x > node2.x)
            && ((node2.x + node2.l) > node1.x))) // Overlaps x, check y
    {
      overlapX = true;
    }

    if (overlapX
            && ((node1.y == node2.y)
            || ((node1.y < node2.y)
            && ((node1.y + node1.w) > node2.y))
            || ((node1.y > node2.y)
            && (node1.y < (node2.y + node2.w))))) {
      overlap = true;
    }

    return overlap;
  }

  public void save(String outputDir, String xmlName) throws IOException {

    Iterator<Component> iter = cfg.components.values().iterator();
    while (iter.hasNext()) {
      Component cCfg = iter.next();
      cCfg.xMin = cCfg.x;
      cCfg.xMax = cCfg.x;
      cCfg.yMin = cCfg.y;
      cCfg.yMax = cCfg.y;
      cCfg.zMin = cCfg.z;
      cCfg.zMax = cCfg.z;
    }
    String newXmlFilePathSuffix = "_" + this.getClass().getSimpleName() + "_W" + this.wireWeight + "_T" + this.temperatureWeight;
    newXmlFilePathSuffix += ".xml";

    String newOutputFile = outputDir + File.separator + xmlName.replaceAll(".xml", newXmlFilePathSuffix);
    String oldXmlFilePath = cfg.xmlFilePath;

    cfg.xmlFilePath = newOutputFile;
    cfg.save();

    cfg.xmlFilePath = oldXmlFilePath;

  }

  // Test code for codification method
  public static void main(String[] args) {

    String xmlFilePath = "test_fp" + File.separator + "ejemplo2.xml";
    String outDir = "test_fp" + File.separator + "sols";

    FloorplanConfiguration cfg = new FloorplanConfiguration(xmlFilePath);

    // Create floorplan using DTS notation
    FloorplanDTS cfgDTS = new FloorplanDTS(cfg, 1.0, 0.0, 0, true, outDir);

    System.out.println(cfgDTS);

  }
}
