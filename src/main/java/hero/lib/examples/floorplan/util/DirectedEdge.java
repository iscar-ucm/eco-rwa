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

import hero.lib.examples.floorplan.Component;

public class DirectedEdge {
    private Component beginEdge;
    private Component endEdge;
    
    public DirectedEdge(Component beginNode, Component endNode){
        beginEdge = beginNode;
        endEdge = endNode;
    }
    
    public Component getBegin() {
        return beginEdge;
    }

    public Component getEnd() {
        return endEdge;
    }
    
    public void setBegin(Component beginNode) {
        beginEdge = beginNode;
    }

    public void setEnd(Component endNode) {
        endEdge = endNode;
    }

    public void setEdge(Component beginNode, Component endNode) {
        beginEdge = beginNode;
        endEdge = endNode;
    }
    
    public void invertEdge() {
        Component aux = beginEdge;
        beginEdge = endEdge;
        endEdge = aux;
    }

    public boolean isEqual(DirectedEdge edgeRef) {
        boolean equalEdges = false;
        if (this.beginEdge == edgeRef.beginEdge && this.endEdge == edgeRef.endEdge) {
            equalEdges = true;
        }
        return equalEdges;
    }

    public void show() {
        System.out.println(beginEdge + "->" + endEdge);
    }
}

/**************************************************************************
 *
 * @author cia  ------------ 1a version --------------
 */
/*
public class DirectedEdge {
    
    private int beginEdge;
    private int endEdge;
    
    public DirectedEdge(){
        beginEdge = -1;
        endEdge = -1;
    }
    
    public DirectedEdge(int idBeginEdge, int idEndEdge){
        beginEdge = idBeginEdge;
        endEdge = idEndEdge;   
    }
    
    public int getBegin(){
        return beginEdge;
    }
    
    public int getEnd(){
        return endEdge;
    }
    
    public void setBegin(int idNode){
        beginEdge = idNode;
    }
    
    public void setEnd(int idNode){
        endEdge = idNode;
    }
    
    public void setEdge(int idBeginEdge, int idEndEdge){
        beginEdge = idBeginEdge;
        endEdge = idEndEdge;        
    }
    
    public void invertEdge(){
        int aux = beginEdge;
        beginEdge = endEdge;
        endEdge = aux;
   }
    
    public boolean isEqual(DirectedEdge edgeRef){
        boolean equalEdges=false;
        if (this.beginEdge==edgeRef.beginEdge && this.endEdge==edgeRef.endEdge)
            equalEdges=true;
        return equalEdges;
    }
    
    public void show(){
        System.out.println(beginEdge + "->" + endEdge );
    }
}
*/