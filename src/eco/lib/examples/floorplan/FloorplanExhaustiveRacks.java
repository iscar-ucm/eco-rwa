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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jlrisco
 */
public class FloorplanExhaustiveRacks extends FloorplanExhaustive {

    public FloorplanExhaustiveRacks(String xmlFilePath) {
        super(xmlFilePath);
    }

    @Override
    public double fitnessTemp(int idx, int xx, int yy, int zz) {
        double fitness = 0.0;
        double[] tempObjs = new double[cfg.numPowerProfiles];
        for (int i = 0; i < tempObjs.length; ++i) {
            tempObjs[i] = 0.0;
        }

        Component cI = components.get(idx);
        Component cJ = null;
        double dist = 0.0;
        for (int j = 0; j < idx; ++j) {
            cJ = components.get(j);
            // Minimiza temperaturas máximas
            if (zz == cJ.z) {
                dist = Math.sqrt(Math.pow(xx + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(yy + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(zz + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2));
                // Potencia: Minimiza temperaturas máximas
                if (dist != 0) {
                    for (int p = 0; p < tempObjs.length; ++p) {
                        tempObjs[p] += ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP)) / dist;
                    }
                }
            }
        }
        for (int i = 0; i < tempObjs.length; ++i) {
            fitness += tempObjs[i];
        }
        fitness /= tempObjs.length;
        return fitness;
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            args = new String[1];
            args[0] = "D:\\jlrisco\\Trabajo\\Investiga\\Estudiantes\\Zorana\\PaperRevista\\10x_opt_arm_30_mips_30.xml";
            //args[1] = "10";
            //args[2] = "";
            System.out.println("java -jar ExhaustiveSearchRacks.jar XmlFilePath");
            return;
        }
        String fileName = args[0];
        FloorplanExhaustiveRacks search = new FloorplanExhaustiveRacks(fileName);
        search.run();
        try {
            search.cfg.xmlFilePath = search.cfg.xmlFilePath.replaceAll(".xml", "_exse.xml");
            search.cfg.save();
        } catch (IOException ex) {
            Logger.getLogger(FloorplanExhaustiveRacks.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
