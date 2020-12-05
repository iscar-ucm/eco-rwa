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

import hero.lib.examples.floorplan.util.DirectedAcyclicGraph;
import hero.lib.examples.floorplan.util.DirectedEdge;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Random;
//import jeco.lib.problems.floorplan.FloorplanConfiguration;

/**
 * @author cia
 */
public class SequencePair {

    private ArrayList<Component> gammaP = new ArrayList<Component>();
    private ArrayList<Component> gammaN = new ArrayList<Component>();
    private DirectedAcyclicGraph graphH = new DirectedAcyclicGraph();
    private DirectedAcyclicGraph graphV = new DirectedAcyclicGraph();
    private FloorplanConfiguration cfg;
    private int layer;
    /*              
     * This class implements the decodification of a 2D 'Sequence Pair'
     *   Therefore, the height of the components is not considered.
     * A 'sequence pair' codification consists of 2 sequences {gammaP,gammaN} of 
     *   components. Decoding them one obtains 2 graphs {grapH,graphV}. 
     *   Then, together with the configuration data given in 'cfg', one computes
     *   the coordinates of point G, and then of point O (see figure below).
     * 
     *           /----/|    O = characteristic point                             
     *    Z     /----/ |        of the component.                              
     *    |_Y   | |  | |    G = point used to compute 'o'                                 
     *    /     | O__|_|            o = G - (l,w,h)                                   
     *   X      |/   | /    X = length (l)                                       
     *          /----G/     Y = width  (w)                                       
     *                      Z = heigth (h) <- not considered in 2D   
     * 
     * In order to make it usefull for 3D chips the member 'layer' is introduced
     */

    public SequencePair(FloorplanConfiguration configuration,
            ArrayList<Component> gamma) {
        /* FIXED Constructor 
         * of the Floorplan with sequence pair (SP) codification 
         * given a single gamma sequence.
         * Obviously both gammaN and gammaP = gamma.
         * 
         * Then {graphH, graphV} are recovered from the SP codification
         *  with 'recoverGraphs()'
         * Finally the (x,y) of each component in 'cfg' are obtained 
         *  with 'decodeGraphs()'
         * All the components are insterted in the layer 0
         */
        cfg    = configuration;
        layer  = 0;
        for (int k=0; k<gamma.size();k++){
            Component cAux = gamma.get(k).clone();
            cAux.z=layer;
            gammaN.add(cAux);
            gammaP.add(cAux);
        }
        this.recoverGraphs();
        this.decodeGraphs();
    }

    public SequencePair(FloorplanConfiguration configuration, int zValue,
            ArrayList<Component> gammaN, ArrayList<Component> gammaP ){
        /* FIXED Constructor 
         * of the Floorplan with sequence pair (SP) codification 
         * given both gammaN and gammaP sequences.
         * 
         * Then {graphH, graphV} are recovered from the SP codification
         *  with 'recoverGraphs()'
         * Finally the (x,y) of each component in 'cfg' are obtained 
         *  with 'decodeGraphs()'
         * All the components are insterted in the layer = zValue
         */
        cfg = configuration;
        layer = zValue;
        for (int k = 0; k < gammaN.size(); k++) {
            Component cAux = gammaN.get(k).clone();
            cAux.z = layer;
            gammaN.add(cAux);
        }
        for (int k = 0; k < gammaP.size(); k++) {
            Component cAux = gammaP.get(k).clone();
            cAux.z = layer;
            gammaP.add(cAux);
        }
        this.recoverGraphs();
        this.decodeGraphs();
    }

    public SequencePair(FloorplanConfiguration configuration,
            ArrayList<Component> gamma, int n, int zValue) {
        /* FIXED+RANDOM Constructor 
         * of the Floorplan with sequence pair (SP) codification 
         * given a gamma sequence, an two integers n and zValue.
         * 
         * 'gamma' is an ArrayList of components out of which 'n' will be 
         *   randomly chosen for generating 'gammaN'. 
         * Then 'gammaN' is randomly shuffled for generating 'gammaP'.
         * Components chosen are removed from gamma.
         * 
         * Then {graphH, graphV} are recovered from the SP codification
         *  with 'recoverGraphs()'.
         * Finally the (x,y) of each component is obtained with 'decodeGraphs()'
         *  and all are placed in same layer z = 'zValue'.
         */
        cfg = configuration;
        Random generator = new Random();
        for (int k = 0; k < n; k++) {
            int pos = generator.nextInt(gamma.size());
            gamma.get(pos).z = zValue;
            gammaP.add(gamma.get(pos));
            gammaN.add(gamma.get(pos));
            gamma.remove(pos);
        }
        Iterator<Component> iter = configuration.components.values().iterator();
        for (int k = 0; k < gammaN.size(); k++) {
            Component c = iter.next();
            int pos = generator.nextInt(gammaN.size());
            c = gammaN.get(pos);
            gammaN.add(c);
            gammaN.remove(pos);
        }

        this.recoverGraphs();
        this.decodeGraphs();
    }

    
    public SequencePair(FloorplanConfiguration configuration, int zValue) {
        /* RANDOM Constructor 
         * of a Floorplan with sequence pair (SP) codification 
         * given an already existing configuration cfg.
         * 
         * The SP codification {gammaN, gammaP} is taken directly from 
         *  the sequential readout of those components of 'cfg' in 'zValue'.
         * Therefore it is very unlikey that the SP constructed 
         *   codifies the layer properly. That is why it is a random construct.
         * So this constructor should be used only for creating the object, 
         *   and always followed by setSP for instance
         * Then {graphH, graphV} are recovered from the SP codification
         *  with 'recoverGraphs()'
         * Finally the (x,y) of each component in 'cfg' are obtained 
         *  with 'decodeGraphs()'
         */
        cfg = configuration;
        Iterator<Component> iter = configuration.components.values().iterator();
        while (iter.hasNext()) {
            Component c = iter.next().clone();
            if (c.z == layer) {
                gammaP.add(c);
                gammaN.add(c);
            }
        }
        this.recoverGraphs();
        this.decodeGraphs();
        this.layer = layer;
    }

    public SequencePair(FloorplanConfiguration configuration,
            ArrayList<Integer> doublePhenotype, int zValue) {
        cfg = configuration;
        layer = zValue;
        int offset = doublePhenotype.size() / 2;
        for (int k = 0; k < offset; k++) {
            cfg.components.get( doublePhenotype.get(k) ).z = layer;
            gammaN.add(cfg.components.get( doublePhenotype.get(k) ) );
            gammaP.add(cfg.components.get( doublePhenotype.get(k + offset) ) );
        }
        this.recoverGraphs();
        this.decodeGraphs();
    }

    private void recoverGraphs() {
        /* Construction of horizontal (graphH) and vertical (graphV) 
         * {origen, destino} are id contained in gammaN y gammaP.
         * {pivot, posicion} are indixes of gammaP.
         */
        DirectedAcyclicGraph newGraphH = new DirectedAcyclicGraph();
        DirectedAcyclicGraph newGraphV = new DirectedAcyclicGraph();
        int m = gammaP.size();
        for (int k1 = 0; k1 < m - 1; k1++) {
            int origen, pivot;
            origen = k1;
            pivot = gammaP.indexOf(gammaN.get(origen));
            for (int k2 = k1 + 1; k2 < m; k2++) {
                int destino, posicion;
                destino = k2;
                posicion = gammaP.indexOf(gammaN.get(destino));
                if (posicion < pivot) {
                    DirectedEdge vertice = new DirectedEdge(gammaN.get(origen), gammaN.get(destino));
                    newGraphV.addEdge(vertice);
                } else {
                    DirectedEdge vertice = new DirectedEdge(gammaN.get(origen), gammaN.get(destino));
                    newGraphH.addEdge(vertice);
                }
            }
        }
        graphH = newGraphH;
        graphV = newGraphV;
    }

    private void decodeGraphs() {
        /* Given the horizontal and vertical graphs, and the map of components 
         *  with their dimensions(l,w,h), we can decode the graph and obtain 
         *  the coordinates of each characteristic point (x,y,z)
         * 
         * {nodeListH, nodeListV} are auxiliar, internal ArrayLists that 
         *  indicate the order in which coordinates of the components should 
         *  be computed. Both are created out of gammaP.
         * !!intuition!! nodeListH=gammaP, nodeListV=gammaN; but nevertheless
         *  this code launches 'getNodeList' to ensure it works.   
         */
        ArrayList<Component> nodeListH = new ArrayList<Component>();
        ArrayList<Component> nodeListV = new ArrayList<Component>();

        Iterator<Component> iter = gammaP.iterator();
        while (iter.hasNext()) {
            Component c = iter.next();
            nodeListH.add(c);
            nodeListV.add(c);
        }
        getNodeList(graphH, nodeListH);
        getNodeList(graphV, nodeListV);
        getCoordinates(nodeListH, nodeListV);
    }

    private void getNodeList(DirectedAcyclicGraph graph,
            ArrayList<Component> nodeList) {
        /* This method reorders the 'nodeList' according to the dag 'graph' so 
         *  if node 'i' is prior to 'j' in the graph, so it is in the 'nodeList'
         */
        for (int k = 0; k < graph.numOfEdges(); k++) {
            int beginNodePosition =
                    nodeList.indexOf(graph.getEdge(k).getBegin());
            int endNodePosition =
                    nodeList.indexOf(graph.getEdge(k).getEnd());
            if (beginNodePosition > endNodePosition) {
                Component auxComponent = nodeList.remove(endNodePosition);
                nodeList.add(beginNodePosition, auxComponent);
            }
        }
    }

    private void getCoordinates(ArrayList<Component> nodeListH,
            ArrayList<Component> nodeListV) {
        /*This method computes the coordinates (x,y,z) of each component in the 
         * given configuration map 'cfg' after having decoded a SP codification
         * {gammaN, gammaP}.
         *It also needs the order given in 'nodeListH' and 'nodeListV'
         */

        // This FOR computes point G, coord x
        for (int k1 = 0; k1 < nodeListH.size(); k1++) {
            nodeListH.get(k1).x = nodeListH.get(k1).l;
            for (int k2 = 0; k2 < graphH.numOfEdges(); k2++) {
                if (graphH.getEdge(k2).getEnd() == nodeListH.get(k1)) {
                    int totalDistance =
                            graphH.getEdge(k2).getBegin().x
                            + graphH.getEdge(k2).getEnd().l;
                    nodeListH.get(k1).x =
                            Math.max(totalDistance, nodeListH.get(k1).x);
                }
            }
        }
        // This FOR computes point o, coord x
        for (int k1 = 0; k1 < nodeListH.size(); k1++) {
            nodeListH.get(k1).x = nodeListH.get(k1).x - nodeListH.get(k1).l;
        }
        //---------------------------------------------------------
        // This FOR computes point G, coord y
        for (int k1 = 0; k1 < nodeListV.size(); k1++) {
            nodeListV.get(k1).y = nodeListV.get(k1).w;
            for (int k2 = 0; k2 < graphV.numOfEdges(); k2++) {
                if (graphV.getEdge(k2).getEnd() == nodeListV.get(k1)) {
                    int totalDistance =
                            graphV.getEdge(k2).getBegin().y
                            + graphV.getEdge(k2).getEnd().w;
                    nodeListV.get(k1).y =
                            Math.max(totalDistance, nodeListV.get(k1).y);
                }
            }

        }
        // This FOR computes point o, coord y
        for (int k1 = 0; k1 < nodeListV.size(); k1++) {
            nodeListV.get(k1).y = nodeListV.get(k1).y - nodeListV.get(k1).w;
        }

    }

    public ArrayList<Component> getGamma(char np) {
        ArrayList<Component> gamma = new ArrayList<Component>();
        if (np == 'n' || np == 'N') {
            gamma = gammaN;
        } else if (np == 'p' || np == 'P') {
            gamma = gammaP;
        } else {
            System.out.println("Error. Must be N or P");
        }
        return gamma;
    }

    public FloorplanConfiguration getConfig() {
        return this.cfg;
    }

    public void setConfig(FloorplanConfiguration otherCfg) {
        cfg = otherCfg;
    }

    public void setSP(ArrayList<Component> newGammaN,
            ArrayList<Component> newGammaP) {
        gammaN = newGammaN;
        gammaP = newGammaP;
        this.recoverGraphs();
        this.decodeGraphs();
    }

    public int size() {
        return gammaN.size();
    }

    public void show(boolean flagGN, boolean flagGP,
            boolean flagGraphs, boolean flagFP) {
        /* print out of all the data
         */
        if (flagGN) {
            System.out.println();
            System.out.println("-gammaN--------------------");
            Iterator<Component> iter = gammaN.iterator();
            while (iter.hasNext()) {
                Component c = iter.next();
                System.out.print(c.id + ",");
            }
        }
        if (flagGP) {
            System.out.println();
            System.out.println("-gammaP--------------------");
            Iterator<Component> iter = gammaP.iterator();
            while (iter.hasNext()) {
                Component c = iter.next();
                System.out.print(c.id + ",");
            }
        }
        if (flagGraphs) {
            System.out.println();
            System.out.println("-graphH--------------------");
            graphH.show();
            System.out.println("-graphV--------------------");
            graphV.show();
        }
        if (flagFP) {
            System.out.println("-Floorplanning-------------");
            Iterator<Component> iter = cfg.components.values().iterator();
            while (iter.hasNext()) {
                Component c = iter.next();
                System.out.println("C." + c.id + "=" + "(" + c.x + "," + c.y + "," + c.z + ")");
            }
        }
    }
    
    public ArrayList<Integer> getDoublePhenotype() {
        ArrayList<Integer> doublePhenotype = new ArrayList<Integer>();
        int sizeOfGamma = this.gammaN.size();
        for (int i = 0; i < sizeOfGamma; i++) {
            doublePhenotype.add(this.gammaN.get(i).id);
        }
        for (int i = 0; i < sizeOfGamma; i++) {
            doublePhenotype.add(this.gammaP.get(i).id);
        }
        return doublePhenotype;
    }

    public SequencePair clone() {
        /* The 'clone' method returns a copycat
         * It uses the constructor with the following parameters
         * -- configuration 
         * -- doublePhenotype
         * -- numLayer
         */
        int numLayer = this.layer;

        ArrayList<Integer> doublePhenotype = new ArrayList<Integer>();
        int sizeOfGamma = this.gammaN.size();
        for (int i = 0; i < sizeOfGamma; i++) {
            doublePhenotype.add(this.gammaN.get(i).id);
        }
        for (int i = 0; i < sizeOfGamma; i++) {
            doublePhenotype.add(this.gammaP.get(i).id);
        }

        SequencePair clonedSP = new SequencePair(
                this.cfg, doublePhenotype, numLayer);

        return clonedSP;
    }

    public static void main(String[] args) {
        
    }
}//end of Class

/* ************************************************************************
 * @author cia ------ 1ª Version ------
 * (usaba la clase 'sequence' que ha quedado obsoleta)
 *
public class FloorplanSP {
private int m;
private Sequence gammaP = new Sequence();
private Sequence gammaN = new Sequence();
private DirectedAcyclicGraph gh = new DirectedAcyclicGraph();
private DirectedAcyclicGraph gv = new DirectedAcyclicGraph();
private ArrayList<Integer> dimX = new ArrayList<Integer>();
private ArrayList<Integer> dimY = new ArrayList<Integer>();
private ArrayList<Integer> coordX = new ArrayList<Integer>();
private ArrayList<Integer> coordY = new ArrayList<Integer>();
private FloorplanConfiguration cfg;

public FloorplanSP(){
gammaP.fixed6Fill(15, 22, 43, 59, 32, 64);
gammaN.fixed6Fill(32, 22, 64, 15, 43, 59);
this.fillDims();
m = gammaP.size();

Iterator<Component> iter = cfg.components.values().iterator();
while (iter.hasNext()) {
Component c = iter.next();

}
//cfg.components.get("e")
}

public FloorplanSP(FloorplanConfiguration cfg) {
this();
this.cfg = cfg;

}


public FloorplanSP(int m){
gammaP.randomFill(m);
gammaN.randomFill(m);
this.fillDims();
}

public void recoverGraphs() {
// 
// Construccion de los grafos horizontal (gh) y vertical (gv)
//
for (int k1 = 0; k1 < m - 1; k1++) {
Integer origen, pivot;
origen = gammaN.get(k1);
pivot = gammaP.getFirst(origen);
for (int k2 = k1 + 1; k2 < m; k2++) {
Integer destino, posicion;
destino = gammaN.get(k2);
posicion = gammaP.getFirst(destino);
if (posicion < pivot) {
DirectedEdge vertice = new DirectedEdge(origen, destino);
gv.addEdge(vertice);
} else {
DirectedEdge vertice = new DirectedEdge(origen, destino);
gh.addEdge(vertice);
}
}
}
}

public void decodeGraphs(){
Sequence nodeListH = new Sequence(); 
Sequence nodeListV = new Sequence();   
nodeListH.fixed6Fill(64, 59, 43, 32, 22, 15);
nodeListV.fixed6Fill(64, 59, 43, 32, 22, 15);

getNodeList(gh, nodeListH); 
getNodeList(gv, nodeListV); 
getCoordinates(gh, nodeListH, dimX, coordX);
getCoordinates(gv, nodeListV, dimY, coordY);
}

public void show(){
System.out.print("Positive ");
gammaP.show();
System.out.print("Negative ");
gammaN.show();
System.out.print("Horizontal");
gh.showEdges();
System.out.print("Vertical");
gv.showEdges();
System.out.println("Coordenada X");
System.out.println(coordX);
System.out.println("Coordenada Y");
System.out.println(coordY);
}

private void getNodeList(DirectedAcyclicGraph graph, Sequence nodeList){
for (int k = 0; k < graph.numOfEdges(); k++) {
int beginNode = graph.getEdge(k).getBegin();
int endNode = graph.getEdge(k).getEnd();
int beginNodePosition = nodeList.getFirst(beginNode);
int endNodePosition = nodeList.getFirst(endNode);
if (beginNodePosition > endNodePosition) {
nodeList.move(beginNodePosition, endNodePosition);
}
}
}

private void getCoordinates(
DirectedAcyclicGraph graph, Sequence           nodeList,
ArrayList<Integer>   dims,  ArrayList<Integer> coords){

for (int k1 = 0; k1 < dims.size(); k1++) {
Integer currentNode = nodeList.get(k1);
coords.add(k1,0);
for (int k2 = 0; k2 < graph.numOfEdges(); k2++) {
if (graph.getEdge(k2).getEnd() == currentNode) {
Integer prevNode = graph.getEdge(k2).getBegin();
//int positionPrevNode = 
int aux = Math.max(coords.get(k1), coords.get(prevNode));
coords.set(k1, aux);                    
}
}
coords.set(k1, coords.get(k1) + dims.get(k1));
}

}

private void fillDims(){
//dim1=[2 2 3 2 1 2];dim2=[2 1 1 3 3 2];
dimX.add(2);dimX.add(2);dimX.add(3);
dimX.add(2);dimX.add(1);dimX.add(2);

dimY.add(2);dimY.add(1);dimY.add(1);
dimY.add(3);dimY.add(3);dimY.add(2);
}
}

 */
