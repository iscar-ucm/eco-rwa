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

import hero.lib.examples.floorplan.util.Solution;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This interface provides some specific elements for FlooplanSolutions
 * 
 * @author J. M. Colmenar
 */
public abstract class FloorplanSolution implements Solution {
    
    /** Values of starting solution */
    public double startingWiring = 0.0;
    public double startingTemp = 0.0;
    
    
    /** The configuration is needed in order to compute couplings **/
    public FloorplanConfiguration cfg = null;
    
    /**
     * Computes the wiring (Manhattan distance) for the wiring of the flooplan
     *
     * @return total wire length
     */
    protected double computeWiring() {
        return this.cfg.computeWireObj();
    }


    /**
     * Computes the temperature of the flooplan
     *
     * @return temperature
     */
    protected double computeTemperature() {
        // TODO: always considering just one power profile
        return this.cfg.computeTempObj()[0];
    }
    
    
    /** Logs execution time and objective values to file */      
    public void logObjectives(double time, String fileName) {
       // File log code
       try {
           // Appending
           BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName),true));
           
           double obj = getObjective();
           
           // # Time Wiring Temperature Feasible Obj.Value
           writer.write(time + " " + computeWiring() + " " + computeTemperature() + " "+ (isFeasible() ? "1":"0") + " " + obj + "\n");
           writer.close();
        } catch (IOException ex) {
           Logger.getLogger(FloorplanDTS.class.getName()).log(Level.SEVERE, null, ex);
        }          
    }    
    
    
    
    
    /* --------------- From now on, do not modify --------------- */
    
    public boolean isFeasible() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isComplete() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getObjective() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getBound() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Enumeration<Solution> getSuccessors() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Solution clone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int compareTo(Solution t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
