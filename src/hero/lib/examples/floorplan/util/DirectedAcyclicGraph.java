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

import java.util.ArrayList;
import java.util.Iterator;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author cia
 */

public class DirectedAcyclicGraph{ 
    
    private ArrayList<DirectedEdge> dag = new ArrayList<DirectedEdge>();
    /*
     * A Directed Acyclic Graph (dag) is an ArrayList of Directed Edges
     * This class includes the following methods
     *  - addEdge
     *  - getEdge
     *  - getFirstPosition
     *  - getFirstPositionFrom
     *  - removeEdge
     *  - removeFirstEdge
     *  - moveEdge
     *  - numOfEdges
     *  - show
     */
    
    public void addEdge (DirectedEdge newEdge){
        dag.add(newEdge);
    }
    
    public void addEdge(int position, DirectedEdge newEdge){
        if ( position >=0 )
            dag.add(position, newEdge);
    }
    
    public DirectedEdge getEdge(int position){
        //DirectedEdge myEdge = new DirectedEdge();
        //return myEdge = dag.get(position);
        return dag.get(position);
    }
    
    public int getFirstPosition(DirectedEdge testEdge){
        int position=0;
        boolean edgeFound=false;
        DirectedEdge currentEdge;      
        
        while ( position<dag.size() && !edgeFound ){
            currentEdge = dag.get(position);
            if (currentEdge.getBegin() != testEdge.getBegin())
                position++;
            else if (currentEdge.getEnd() != testEdge.getEnd())
                position++;
            else
                edgeFound=true;
        }
        if (! edgeFound) position=-1;
        return position;               
    }
    
    public int getFirstPositionFrom(int position, DirectedEdge testEdge){
        boolean edgeFound=false;
        DirectedEdge currentEdge;      
        
        while ( position<dag.size() && !edgeFound ){
            currentEdge = dag.get(position);
            if (currentEdge.getBegin() != testEdge.getBegin())
                position++;
            else if (currentEdge.getEnd() != testEdge.getEnd())
                position++;
            else
                edgeFound=true;
        }
        if (! edgeFound) position=-1;
        return position;               
    }
    
    public void removeEdge(int position){
        if ( position >=0 && position < dag.size() )
            dag.remove(position);
    }
    
    public void removeFirstEdge(DirectedEdge testEdge){
        int position=0;
        position=this.getFirstPosition(testEdge); 
        if (position>=0)
        this.removeEdge(position);
    }
    
//    public void removeEdgesFrom(int position, DirectedEdge testEdge){
//        while ( position<dag.size() ){
//            DirectedEdge currentEdge = new DirectedEdge();
//            currentEdge = dag.get(position);
//            if (currentEdge.isEqual(testEdge) )
//                this.removeEdge(position);
//            position++;
//        }
//    }
    
    public void moveEdge(int origin, int destination){
        if ( origin >=0 && origin < dag.size() && 
                destination >=0 && destination < dag.size() ){
            DirectedEdge auxEdge = dag.remove(origin);
            dag.add(destination,auxEdge);   
        }     
    }
    
    public int numOfEdges(){
        return dag.size();
    }
    
    public void show() {           
        System.out.println(".DAG.");
        Iterator<DirectedEdge> iter = dag.iterator();
        while ( iter.hasNext() ) {
            DirectedEdge c = iter.next();
            System.out.println("("+c.getBegin().getId()+","+c.getEnd().getId()+")"); 
        }
     }
} 

