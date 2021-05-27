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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Random;
//import jeco.lib.problems.floorplan.FloorplanConfiguration;

import eco.lib.examples.floorplan.util.Solution;

/**
 * @author cia
 */
public class FloorplanSP extends FloorplanSolution {
    //-axis limits- 

    protected int maxL; // x axis limit (length)
    protected int maxW; // y axis limit (width)
    protected int maxH; // z axis limit (height)
    //-current maxs-
    protected int currMaxLongX = 0;   // Current max long x 
    protected int currMaxWidthY = 0;  // Current max width y  
    protected int currMaxHeightZ = 0; // Current max height Z 
    //-Parameter to avoid re-compute the volume -
    protected boolean volumeComputed = false;
    //-Weights for the objectives:
    private static final double COMMON_WEIGHT = 1.0 / 2.0;
    protected double volumeWeight = COMMON_WEIGHT;
    protected double wireWeight = COMMON_WEIGHT;
    protected double temperatureWeight = COMMON_WEIGHT;
    //-Random number generator-
    private static Random rnd = new Random();
    /** Sequence Pair codification:
     *  There is una sequence pair for each layer in the 3DIC
     *  The representation consists of an ArrayList of SequencePair 
     */
    protected ArrayList<SequencePair> layerSP = new ArrayList<SequencePair>();

// ---------------------------------------------------------------- CONSTRUCTORS  
    public FloorplanSP(FloorplanConfiguration configuration) {
        /* RANDOM CONSTRUCTOR
         * Constructor of a 3D floorplan using a random 'Sequence Pair' 
         *   codification for each layer.
         * An already existing configuration is needed.
         */
        cfg = configuration;        // Process configuration

        maxL = cfg.maxLengthInCells;// Set floorplan x limit
        maxW = cfg.maxWidthInCells; // Set floorplan y limit
        maxH = cfg.numLayers;       // Set floorplan z limit

//        volumeWeight = volumeWgt;   //fitness function weights for volume
//        wireWeight = wireWgt;       //fitness function weights for wire
//        temperatureWeight = tempWgt;//fitness function weights for temperature

        //--FIRST FLOORPLAN--       
        //1st: create 'gamma' a sequence with all the componentes in a row.
        Iterator<Component> iter = configuration.components.values().iterator();
        ArrayList<Component> gamma = new ArrayList<Component>();
        while (iter.hasNext()) {
            Component c = iter.next();
            gamma.add(c);
        }
        //2nd: deal all components among the number of layers randomly
        int n = gamma.size() / configuration.numLayers;
        for (int contZ = 0; contZ < configuration.numLayers; contZ++) {
            if (contZ == configuration.numLayers - 1) {
                n = gamma.size();
            }
            SequencePair spCode = new SequencePair(cfg, gamma, n, contZ);
            layerSP.add(spCode);
            //layerSP.get(contZ).show();  //<-- only for tracking purposes
        }
        //!!! <--- aquí habría que generar spCode adecuadamente ---> !!!
        //--compute volume
        computeVolume();
        //--Starting objective values
        startingWiring = computeWiring();
        startingTemp = computeTemperature();
    }

    public FloorplanSP(FloorplanConfiguration configuration,
            ArrayList<ArrayList<Integer>> listSPcodif) {
        /* FIXED CONSTRUCTOR
         * Constructor of a 3D floorplan using a given 'Sequence Pair' 
         *   codification for each layer.
         * An already existing configuration is needed.
         */
        cfg = configuration;        // Process configuration

        maxL = cfg.maxLengthInCells;// Set floorplan x limit
        maxW = cfg.maxWidthInCells; // Set floorplan y limit
        maxH = cfg.numLayers;       // Set floorplan z limit

        //--FLOORPLAN--       
        for (int k = 0; k < listSPcodif.size(); k++) {
            SequencePair auxSP = new SequencePair(cfg, listSPcodif.get(k), k);
            this.layerSP.add(auxSP);
        }
        //--compute volume
        computeVolume();
        //--Starting objective values
        startingWiring = computeWiring();
        startingTemp = computeTemperature();
    }
    
    public FloorplanSP(FloorplanConfiguration configuration,
            ArrayList<ArrayList<Integer>> listSPcodif,
            double wireWgt, double tempWgt) {
        /* PARAMETRIZED CONSTRUCTOR
         * Constructor of a 3D floorplan using a given 'Sequence Pair' 
         *   codification for each layer.
         * An already existing configuration is needed.
         * It also resets some parameters
         */
        //--call the 'fixed constructor' first
        this(configuration, listSPcodif);
        //--add rest of parameters
        wireWeight = wireWgt;       //fitness function weights for wire
        temperatureWeight = tempWgt;//fitness function weights for temp.
    }

    public FloorplanSP(FloorplanConfiguration configuration,
            ArrayList<ArrayList<Integer>> listSPcodif,
            double wireWgt, double tempWgt, long seed) {
        /* PARAMETRIZED CONSTRUCTOR
         * Similar to the previous one _BUT_
         * it also admits the seed
         */
        //--call the 'fixed constructor' first
        this(configuration, listSPcodif,wireWgt,tempWgt);
        //--add rest of parameters
        rnd = new Random(seed);
    }

// ------------------------------- METHODS MANDATORY DUE TO 'SOLUTION' INTERFACE
    @Override
    public boolean isFeasible() {
        boolean feasible = false;
        long volValue = computeVolume();
        // Must check all the dimensions:
        if ((currMaxLongX <= maxL)
                && (currMaxWidthY <= maxW)
                && (currMaxHeightZ <= maxH)) {
            feasible = true;
        }
//        //for tracking purposes only :
//          System.out.println("= Current :: Max =");
//          System.out.println("L -> " + currMaxLongX   + " :: " + maxL);
//          System.out.println("W -> " + currMaxWidthY  + " :: " + maxW);
//          System.out.println("H -> " + currMaxHeightZ + " :: " + maxH);
//          System.out.println("--Volumen = " + volValue);
//          System.out.println("--factibilidad = " + feasible);
        return feasible;
    }

    @Override
    public boolean isComplete() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public double getObjective() {

        double objective = 0.0;
        long volume = computeVolume();

        if ((wireWeight == 1.0) || (temperatureWeight == 1.0)) {
            objective = ((wireWeight * computeWiring())
                    + (temperatureWeight * computeTemperature()));
        } else {
            objective = ((wireWeight * computeWiring() / startingWiring)
                    + (temperatureWeight * computeTemperature() / startingTemp));
        }

        if (!isFeasible()) {
            double currVolume = (currMaxLongX * currMaxWidthY * (currMaxHeightZ + 1));
            double maxVolume = (maxL * maxW * maxH);
            double penalty = currVolume / maxVolume;
            objective *= penalty;
        }

        return objective;
        /*------old version---------- 
        // The objective is a weighted function:
        long volume = computeVolume();
        double wireLength = this.cfg.computeWireObj();
        double temperature= this.cfg.computeTempObj()[0];
        
        return (volumeWeight * volume) + (wireWeight * wireLength) + 
        (temperatureWeight * temperature);
         */
    }

    @Override
    public double getBound() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public Enumeration<Solution> getSuccessors() {
        ArrayList<Solution> succesors = new ArrayList<>();
        FloorplanSP solution = (FloorplanSP) this.clone();

        // Neighbor is generated through 5 different operations:
        // 1. swap 2 components from layer1 to layer2 = layer1 + {-1,0,+1}
        // 2. swap 2 entire layers
        // 3. extract 1 component from layer1 and insert it in layer2
        // 4. move 2 components in the same layer
        // 5. move 2 components only in one of the gamma seq.

        // Move random node
        boolean printOutTrack=false;
        switch ( rnd.nextInt(5) ) {
        ///*for tracking and testing*/ switch (1) {   
            case 0:
                if (printOutTrack){System.out.println("swapLayers");}
                swapLayers(solution);
                break;
            case 1:
                if (printOutTrack){System.out.println("swapComponents");     }        
                swapComponents(solution);
                break;
            case 2: //case 3: 
                if (printOutTrack){System.out.println("moveComponent");}
                moveComponent(solution);
                break;
            case 3: //case 5: 
                if (printOutTrack){System.out.println("moveComponentSameLayer");}
                moveComponentSameLayer(solution);
                break;
            case 4: //case 7: 
                if (printOutTrack){System.out.println("swapNeighborSameLayer");}         
                swapNeighborSameLayer(solution);
                break;
            case 5:
                if (printOutTrack) {System.out.println("swapNeighborsInGamma");}
                swapNeighborsInGamma(solution);
                break;
        }

        // New volume has to be computed
        solution.volumeComputed = false;
        solution.computeVolume();

        Solution sol = solution;
        succesors.add(sol);

        return Collections.enumeration(succesors);
    }

    @Override
    public Solution clone() {

        ArrayList<ArrayList<Integer>> listSPcodif = new ArrayList<ArrayList<Integer>>();
        for (int k = 0; k < this.layerSP.size(); k++) {
            ArrayList<Integer> doublePhenotypeLayer = this.layerSP.get(k).getDoublePhenotype();
            listSPcodif.add(doublePhenotypeLayer);
        }
        //FloorplanSP clonedSP = new FloorplanSP(this.cfg.clone(), listSPcodif);
        FloorplanSP clonedSP = new FloorplanSP(this.cfg.clone(), listSPcodif, wireWeight, temperatureWeight);
        clonedSP.startingTemp = this.startingTemp;
        clonedSP.startingWiring = this.startingWiring;
        return clonedSP;
    }

    @Override
    public int compareTo(Solution rhs) {
        //throw new UnsupportedOperationException("Not supported yet.");
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

//--------------------------------------------------------------- OTHER METHODS    
    private long computeVolume() {
        if (!volumeComputed) {
            currMaxLongX = 0;
            currMaxWidthY = 0;
            currMaxHeightZ = 0;
            Iterator<Component> iter = cfg.components.values().iterator();
            while (iter.hasNext()) {
                Component c = iter.next();
                long length = c.x + c.l;
                long width = c.y + c.w;
                long height = c.z + c.h;
                if (length > currMaxLongX) {
                    currMaxLongX = (int) length;
                }
                if (width > currMaxWidthY) {
                    currMaxWidthY = (int) width;
                }
                if (height > currMaxHeightZ) {
                    currMaxHeightZ = (int) height;
                }
            }
            volumeComputed = true;
        }
        return (currMaxLongX * currMaxWidthY * currMaxHeightZ);
    }

    public void show() {
        for (int k = 0; k < layerSP.size(); k++) {
            layerSP.get(k).show(true, true, false, false);
        }
    }

    public void swapComponents(FloorplanSP solution) {
        /* This method locates the position of any 2 components in the 
         * gamma- and gamma+ . Then swaps.
         * E.g: swapping 4 and 7
         *  gamma- = 1,2,3,[4],5  --> 1,2,3,[7],5
         *  gamma+ = [4],1,2,3,5  --> [7],1,2,3,5
         *  gamma- = 9,8,[7]      --> 9,8,[4]    
         *  gamma+ = 8,9,[7]      --> 8,9,[4]   
         */
        //1. select layer1 and layer2 = layer1 + {-1,0,1}  randomly
        int layer1, layer2, sizeLayer1, sizeLayer2;
        do {
            layer1 = rnd.nextInt(solution.layerSP.size());
            layer2 = layer1 + rnd.nextInt(3) - 1;
            if (layer2 < 0) {
                layer2 = layer1 + 1;
            } else if (layer2 >= solution.layerSP.size()) {
                layer2 = layer1 - 1;
            }
            // ! Make sure that layer1 and layer2 are not empty !
            sizeLayer1 = solution.layerSP.get(layer1).size();
            sizeLayer2 = solution.layerSP.get(layer2).size();
        } while (sizeLayer1 <= 0 || sizeLayer2 <= 0);
        //2. select pos1n and pos2n, random components in gammaN of layer{1,2}
        SequencePair sp1 = solution.layerSP.get(layer1);
        SequencePair sp2 = solution.layerSP.get(layer2);
        int pos1n = rnd.nextInt(sp1.size());
        int pos2n = rnd.nextInt(sp2.size());
        //3. find pos1p and pos2p, position in gammaP of layer{1,2}
        ArrayList<Component> gamma1n = sp1.getGamma('n');
        ArrayList<Component> gamma1p = sp1.getGamma('p');
        ArrayList<Component> gamma2n = sp2.getGamma('n');
        ArrayList<Component> gamma2p = sp2.getGamma('p');
        Component c1n = gamma1n.get(pos1n);
        int pos1p = gamma1p.indexOf(c1n);
        Component c1p = gamma1p.get(pos1p);
        Component c2n = gamma2n.get(pos2n);
        int pos2p = gamma2p.indexOf(c2n);
        Component c2p = gamma2p.get(pos2p);
        //4.swap
        gamma1n.remove(pos1n);
        gamma1n.add(pos1n, c2n);
        gamma1n.get(pos1n).z = layer1;
        gamma1p.remove(pos1p);
        gamma1p.add(pos1p, c2p);
        gamma1p.get(pos1p).z = layer1;
        gamma2n.remove(pos2n);
        gamma2n.add(pos2n, c1n);
        gamma2n.get(pos2n).z = layer2;
        gamma2p.remove(pos2p);
        gamma2p.add(pos2p, c1p);
        gamma2p.get(pos2p).z = layer2;
        sp1.setSP(gamma1n, gamma1p);
        sp2.setSP(gamma2n, gamma2p);
        solution.layerSP.set(layer1, sp1);
        solution.layerSP.set(layer2, sp2);
    }
    
    public void swapNeighborSameLayer(FloorplanSP solution) {
       //1. select layer1 randomly. (layer2=layer1)
        int layer1, layer2, sizeLayer;
        do {
            layer1 = rnd.nextInt(solution.layerSP.size());
            layer2 = layer1;
            // ! Make sure that layer is not empty !
            sizeLayer = solution.layerSP.get(layer1).size();
        } while (sizeLayer <= 0);
        //1. select pos1n and pos2n, random components in gammaN of layer{1,2}
        SequencePair sp1 = solution.layerSP.get(layer1);
        SequencePair sp2 = solution.layerSP.get(layer2);
        int pos1n = rnd.nextInt(sp1.size());
        int signo = rnd.nextInt(2) - 1; if (signo == 0) {signo = 1;}
        int pos2n = pos1n + signo;
        if (pos2n < 0) { pos2n = pos1n; } else if (pos2n>=sizeLayer) {pos2n = sizeLayer-1;}
        //3. find pos1p and pos2p, position in gammaP of layer{1,2}
        ArrayList<Component> gamma1n = sp1.getGamma('n');
        ArrayList<Component> gamma1p = sp1.getGamma('p');
        ArrayList<Component> gamma2n = sp2.getGamma('n');
        ArrayList<Component> gamma2p = sp2.getGamma('p');
        Component c1n = gamma1n.get(pos1n);
        int pos1p = gamma1p.indexOf(c1n);
        Component c1p = gamma1p.get(pos1p);
        Component c2n = gamma2n.get(pos2n);
        int pos2p = gamma2p.indexOf(c2n);
        Component c2p = gamma2p.get(pos2p);
        //4.swap
        gamma1n.remove(pos1n);
        gamma1n.add(pos1n, c2n);
        gamma1p.remove(pos1p);
        gamma1p.add(pos1p, c2p);
        gamma2n.remove(pos2n);
        gamma2n.add(pos2n, c1n);
        gamma2p.remove(pos2p);
        gamma2p.add(pos2p, c1p);
        sp1.setSP(gamma1n, gamma1p);
        sp2.setSP(gamma2n, gamma2p);
        solution.layerSP.set(layer1, sp1);
        solution.layerSP.set(layer2, sp2);
    }

    public void swapLayers(FloorplanSP solution) {
        /*  This method swaps 2 neighbor layers 
         *  E.g: swapping layers 1 and 2
         *     Layer0  --> Layer0
         *    [Layer1] --> Layer2
         *    [Layer2] --> Layer1
         *     Layer3  --> Layer3
         */
        //1. select layer1 and layer2=layer1 + {-1,1}  randomly
        int layer1 = rnd.nextInt(solution.layerSP.size());
        int signo = rnd.nextInt(2) - 1; if (signo==0) {signo=1;}
        int layer2 = layer1 + signo;
        if (layer2 < 0) {
            layer2 = layer1 + 1;
        } else if (layer2 >= solution.layerSP.size()) {
            layer2 = layer1 - 1;
        }
        //2. swap layers
        SequencePair sp1 = solution.layerSP.get(layer1);
        SequencePair sp2 = solution.layerSP.get(layer2);    
        ArrayList<Component> newGamma2n = sp1.getGamma('n');
        for (int k=0; k<newGamma2n.size();k++){
            newGamma2n.get(k).z=layer2;
        }
        ArrayList<Component> newGamma2p = sp1.getGamma('p');
        for (int k=0; k<newGamma2p.size();k++){
            newGamma2p.get(k).z=layer2;
        }        
        ArrayList<Component> newGamma1n = sp2.getGamma('n');
        for (int k=0; k<newGamma1n.size();k++){
            newGamma1n.get(k).z=layer1;
        }        
        ArrayList<Component> newGamma1p = sp2.getGamma('p');
        for (int k=0; k<newGamma1p.size();k++){
            newGamma1p.get(k).z=layer1;
        }        
        sp1.setSP(newGamma1n, newGamma1p);
        sp2.setSP(newGamma2n, newGamma2p);
        solution.layerSP.set(layer1, sp1);
        solution.layerSP.set(layer2, sp2);
    }
    
public void swapNeighborsInGamma(FloorplanSP solution) {
       //1. select layer randomly.
        int layer, sizeLayer;
        do {
            layer = rnd.nextInt(solution.layerSP.size());
            // ! Make sure that layer is not empty !
            sizeLayer = solution.layerSP.get(layer).size();
        } while (sizeLayer <= 0);
        //2. select gammaN or gammaP
        int gammaSel=rnd.nextInt(2);
        //3. Select pos1, a random components in gammaSel of layer{1,2}
        SequencePair sp = solution.layerSP.get(layer);
        int pos1 = rnd.nextInt(sp.size());
        int signo = rnd.nextInt(2) - 1; if (signo == 0) {signo = 1;}
        int pos2 = pos1 + signo;
        if (pos2 < 0) { pos2 = pos1; } else if (pos2>=sizeLayer) {pos2 = sizeLayer-1;}
        ///*tracking*/System.out.println("track: layer" +layer+ "(" +pos1+","+pos2+")");
        //4. extract gammaSel
        ArrayList<Component> gammaChanged = new ArrayList<Component>();
        ArrayList<Component> gammaFixed = new ArrayList<Component>();
        if (gammaSel==0){
            gammaChanged = sp.getGamma('n');
            gammaFixed = sp.getGamma('p');
        }
        else{
            gammaChanged = sp.getGamma('p');
            gammaFixed = sp.getGamma('n');            
        }
        //5.swap content of selected positions in selected gammas
        Component c1 = gammaChanged.get(pos1);
        Component c2 = gammaChanged.get(pos2);
        gammaChanged.remove(pos1);
        gammaChanged.add(pos1, c2);
        gammaChanged.remove(pos2);
        gammaChanged.add(pos2, c1);
        //set gammas in sp, and sp in solution
        if (gammaSel==0){sp.setSP(gammaChanged, gammaFixed);}
        else            {sp.setSP(gammaFixed, gammaChanged);}        
        solution.layerSP.set(layer, sp);
    }

    public void moveComponent(FloorplanSP solution) {
        /* This method moves one component to a neighbor layer
         * E.g: moving 4 right after 9
         *  gamma- = 1,2,3,[4],5  --> 1,2,3,5
         *  gamma+ = [4],1,2,3,5  --> 1,2,3,5
         *  gamma- = [9],8,7      --> 9,[4],8,7    
         *  gamma+ = 8,[9],7      --> 8,9,[4],7
         */
        //1. select layer1 and layer2 = layer1 + {-1,1}  randomly
        int layer1, layer2, sizeLayer1, sizeLayer2;
        do {
            layer1 = rnd.nextInt(solution.layerSP.size());
            int signo = rnd.nextInt(2) - 1; if (signo == 0) {signo = 1;}
            layer2 = layer1 + signo;
            if (layer2 < 0) {
                layer2 = layer1 + 1;
            } else if (layer2 >= solution.layerSP.size()) {
                layer2 = layer1 - 1;
            }
            // ! Make sure that layer1 and layer2 are not empty !
            sizeLayer1 = solution.layerSP.get(layer1).size();
            sizeLayer2 = solution.layerSP.get(layer2).size();
        } while (sizeLayer1 <= 0 || sizeLayer2 <= 0);
        //2. select pos1n and pos2n, random components in gammaN of layer{1,2}
        SequencePair sp1 = solution.layerSP.get(layer1);
        SequencePair sp2 = solution.layerSP.get(layer2);
        int pos1n = rnd.nextInt(sp1.size());
        int pos2n = rnd.nextInt(sp2.size());
        //3. find pos1p and pos2p, position in gammaP of layer{1,2}
        ArrayList<Component> gamma1n = sp1.getGamma('n');
        ArrayList<Component> gamma1p = sp1.getGamma('p');
        ArrayList<Component> gamma2n = sp2.getGamma('n');
        ArrayList<Component> gamma2p = sp2.getGamma('p');
        Component c1n = gamma1n.get(pos1n);
        int pos1p = gamma1p.indexOf(c1n);
        Component c1p = gamma1p.get(pos1p);
        Component c2n = gamma2n.get(pos2n);
        int pos2p = gamma2p.indexOf(c2n);     
        //Component c2p = gamma2p.get(pos2p);
        //4.move
        gamma1n.remove(pos1n);
        gamma1p.remove(pos1p);
        c1n.z=layer2;
        gamma2n.add(pos2n, c1n);
        c1p.z=layer2;
        gamma2p.add(pos2p, c1p);
        sp1.setSP(gamma1n, gamma1p);
        sp2.setSP(gamma2n, gamma2p);
        solution.layerSP.set(layer1, sp1);
        solution.layerSP.set(layer2, sp2);
    }

    public void moveComponentSameLayer(FloorplanSP solution) {
        //1. select layer1 randomly. (layer2=layer1)
        int layer1, layer2, sizeLayer;
        do {
            layer1 = rnd.nextInt(solution.layerSP.size());
            layer2 = layer1;
            // ! Make sure that layer is not empty !
            sizeLayer = solution.layerSP.get(layer1).size();
        } while (sizeLayer <= 0);
        //1. select pos1n and pos2n, random components in gammaN of layer{1,2}
        SequencePair sp1 = solution.layerSP.get(layer1);
        SequencePair sp2 = solution.layerSP.get(layer2);
        int pos1n = rnd.nextInt(sp1.size());
        int pos2n = rnd.nextInt(sp2.size());
        //3. find pos1p and pos2p, position in gammaP of layer{1,2}
        ArrayList<Component> gamma1n = sp1.getGamma('n');
        ArrayList<Component> gamma1p = sp1.getGamma('p');
        ArrayList<Component> gamma2n = sp2.getGamma('n');
        ArrayList<Component> gamma2p = sp2.getGamma('p');
        Component c1n = gamma1n.get(pos1n);
        int pos1p = gamma1p.indexOf(c1n);
        Component c1p = gamma1p.get(pos1p);
        Component c2n = gamma2n.get(pos2n);
        int pos2p = gamma2p.indexOf(c2n);
        Component c2p = gamma2p.get(pos2p);
        //4.swap
        gamma1n.remove(pos1n);
        gamma1n.add(pos1n, c2n);
        gamma1p.remove(pos1p);
        gamma1p.add(pos1p, c2p);
        gamma2n.remove(pos2n);
        gamma2n.add(pos2n, c1n);
        gamma2p.remove(pos2p);
        gamma2p.add(pos2p, c1p);
        sp1.setSP(gamma1n, gamma1p);
        sp2.setSP(gamma2n, gamma2p);
        solution.layerSP.set(layer1, sp1);
        solution.layerSP.set(layer2, sp2);
    }

    public String save(String xmlFilePath) throws IOException {

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
        cfg.xmlFilePath = xmlFilePath.replaceAll(".xml", newXmlFilePathSuffix);
        cfg.save();

        return newXmlFilePathSuffix;
    }

    public void printCfgPicture(int zoom) {
        Board board = new Board();
        board.cfg = cfg;
        board.zoom = zoom;
        board.buildBoard();
    }

    public static void main(String[] args) {

        boolean original = true;
        int zoom = 11;
        //--base name
        String baseName = "N3HC128"; //"ejemplo_3layers";

    }

}//<-(end of class)

