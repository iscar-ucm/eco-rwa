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
 *  - Josué Pagán Ortíz
 *  - José Luis Risco Martín
 */
package hero.unstable.util.classifier;

/**
 *
 * @author Josué Pagán Ortiz
 */
public class Quantizer {
    protected String kind;
    protected int levels;
    protected double[] steps;
    
    public Quantizer(String kind, int levels) {
        this.kind = kind;
        this.levels = levels;    
        
        steps = new double[levels]; 
        
        switch (kind){
            case "linear":
            default:
                for (int i=0; i<levels; i++) {
                    steps[i] = (double)(i+(0.5));
                }
        }
    }
    
    public int getQ(double iLevel){
        int qLevel = 0;
        if (Double.isNaN(iLevel)) {
            qLevel = levels;
        } else {
            for (int i=levels-1; i>=0; i--){
                if (iLevel > steps[i]){
                    qLevel = (int)Math.ceil(steps[i]);
                    break;
                }
            }
        }
        return qLevel;
    }
}
