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
import java.util.Random;

/**
 *
 * @author cia
 */
public class Sequence {
    //private void isFeasible(){    }
    private ArrayList<Integer> sequence = new ArrayList<Integer>();
    
    public void add(int newValue){
        sequence.add(newValue);
    }
    
    public Integer get(int position){
        Integer output=sequence.get(position);
        return output;
    }
    
    public int getFirst(Integer value){
        int output=-1;
        boolean isfound=false;
        int cont=0;
        
        while ( !isfound && cont<sequence.size() ){
            if (value==sequence.get(cont)){
                output=cont; 
                isfound=true;
            }
            cont++;
        }
        return output;    
    }
    
    public void show(){
        System.out.print("Sequence: ");
        for (int k=0; k<sequence.size(); k++){
            System.out.print(sequence.get(k) + ",");     
        }
        System.out.println();
    }
    
    public void move(int origin, int destination){
        if (origin >= 0 && origin < sequence.size()
                && destination >= 0 && destination < sequence.size()) {
            Integer aux = sequence.remove(origin);
            sequence.add(destination, aux);
        }
    }
    
    public int size(){
        return sequence.size();
    }
    
    //----------------------------------------------------------------------
    public void randomFill(int m) {
        ArrayList<Integer> auxSequence = new ArrayList<Integer>();
        Random generator = new Random();

        for (int k = 0; k < m; k++) {
            auxSequence.add(k);
        }
        for (int k = 0; k < m; k++) {
            int pos = generator.nextInt(auxSequence.size());
            sequence.add(auxSequence.get(pos) + 10);
            auxSequence.remove(pos);
        }
    }

    public void fixed6Fill(
            Integer v0, Integer v1, Integer v2, 
            Integer v3, Integer v4, Integer v5) {
        sequence.add(v0);
        sequence.add(v1);
        sequence.add(v2);
        sequence.add(v3);
        sequence.add(v4);
        sequence.add(v5);
    }
}
