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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jlrisco
 */
public class FloorplanExhaustive {

    protected FloorplanConfiguration cfg;
    protected ArrayList<Component> components = new ArrayList<Component>();
    protected boolean[][][] freeCells;

    public FloorplanExhaustive(String xmlFilePath) {
        cfg = new FloorplanConfiguration(xmlFilePath);
        for (Component c : cfg.components.values()) {
            components.add(c);
        }
        freeCells = new boolean[cfg.maxLengthInCells][cfg.maxWidthInCells][cfg.numLayers];
        int x = 0, y = 0, z = 0;
        for (x = 0; x < cfg.maxLengthInCells; ++x) {
            for (y = 0; y < cfg.maxWidthInCells; ++y) {
                for (z = 0; z < cfg.numLayers; ++z) {
                    freeCells[x][y][z] = true;
                }
            }
        }
        Collections.sort(components, new ComponentThermalComparator());
    }

    public void run() {
        for (int i = 0; i < components.size(); ++i) {
            place(i);
        }
    }

    public boolean place(int idx) {
        Component component = components.get(idx);
        System.out.print(component.toString() + " ... ");
        int bestX = -1;
        int bestY = -1;
        int bestZ = -1;
        double currentObj = Double.POSITIVE_INFINITY, bestObj = Double.POSITIVE_INFINITY;
        int x = 0, y = 0, z = 0;
        for (z = component.zMin; z <= component.zMax; ++z) {
            for (x = component.xMin; x <= component.xMax; ++x) {
                for (y = component.yMin; y <= component.yMax; ++y) {
                    if (freeCells[x][y][z] && feasible(idx, x, y, z)) {
                        // Calculamos el objetivo
                        if (component.type == 0) {
                            currentObj = fitnessTemp(idx, x, y, z);
                        } else {
                            currentObj = fitnessWire(idx, x, y, z);
                        }
                        if (currentObj < bestObj) {
                            bestObj = currentObj;
                            bestX = x;
                            bestY = y;
                            bestZ = z;
                        }
                    }
                }
            }
        }
        component.x = bestX;
        component.y = bestY;
        component.z = bestZ;
        System.out.println("(" + component.x + ", " + component.y + ", " + component.z + ")");
        if (bestX < 0 || bestY < 0 || bestZ < 0) {
            return false;
        }
        for (x = component.x; x < component.x + component.l; ++x) {
            for (y = component.y; y < component.y + component.w; ++y) {
                freeCells[x][y][bestZ] = false;
            }
        }
        return true;
    }

    public double fitnessTemp(int idx, int xx, int yy, int zz) {
        double[] tempObjs = new double[cfg.numPowerProfiles];
        for (int i = 0; i < tempObjs.length; ++i) {
            tempObjs[i] = 0.0;
        }
        Component cI = components.get(idx);
        Component cJ = null;
        double dist = 0.0;
        for (int j = 0; j < idx; ++j) {
            cJ = components.get(j);
            dist = Math.sqrt(Math.pow(xx + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(yy + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(zz + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2));
            if (dist != 0) {
                for (int p = 0; p < tempObjs.length; ++p) {
                    // Potencia: Minimiza temperaturas máximas
                    tempObjs[p] += ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP)) / dist;
                    // Distancia: Minimiza temperaturas promedio
                    // tempObjs[p] -= dist / ((cI.dps[p] / cfg.maxDP) * (cJ.dps[p] / cfg.maxDP));
                }
            }
        }
        // Me quedo con el mayor de los objetivos
        double fitness = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < tempObjs.length; ++i) {
            if (tempObjs[i] > fitness) {
                fitness = tempObjs[i];
            }
        }
        return fitness;
    }

    public double fitnessWire(int idx, int xx, int yy, int zz) {
        double fitness = 0.0;
        int idFrom, idTo;
        Component cI = components.get(idx);
        idFrom = cI.id;
        // Conexiones:
        Component cJ = null;
        double dx = 0, dy = 0, dz = 0;
        for (int j = 0; j < idx; ++j) {
            cJ = components.get(j);
            idTo = cJ.id;
            if ((cfg.couplings.containsKey(idFrom) && cfg.couplings.get(idFrom).contains(idTo)) ||
                    (cfg.couplings.containsKey(idTo) && cfg.couplings.get(idTo).contains(idFrom))) {
                dx = Math.abs(cJ.x + cJ.l / 2.0 - xx - cI.l / 2.0);
                dy = Math.abs(cJ.y + cJ.w / 2.0 - yy - cI.w / 2.0);
                dz = Math.abs(cJ.z - zz);
                // Penalizamos un montón que estén en distinta capa, para que las memorias caigan al lado del core y no encima o debajo
                fitness += (dx + dy + (cfg.numLayers + cfg.maxWidthInCells) * dz);
            }
        }
        return fitness;
    }

    public boolean feasible(int idx, int xx, int yy, int zz) {
        Component component = components.get(idx);
        // Límites del chip
        if (xx + component.l > cfg.maxLengthInCells) {
            return false;
        }
        if (yy + component.w > cfg.maxWidthInCells) {
            return false;
        }
        if (zz + component.h > cfg.numLayers) {
            return false;
        }
        // No puede haber nada de por medio
        Component c = null;
        double dx = 0, dy = 0;
        for (int j = 0; j < idx; ++j) {
            c = components.get(j);
            if (c.z != zz) {
                continue;
            }
            dx = Math.abs(c.x + c.l / 2.0 - xx - component.l / 2.0);
            dy = Math.abs(c.y + c.w / 2.0 - yy - component.w / 2.0);
            if (dx < (c.l / 2.0 + component.l / 2.0) && dy < (c.w / 2.0 + component.w / 2.0)) {
                return false;
            }
        }
        return true;
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            args = new String[1];
            args[0] = "D:\\jlrisco\\Trabajo\\Investiga\\Estudiantes\\DavidCuesta\\benchmarks\\ComparaTavgVsTmax\\NiagaraC48L4.xml";
            System.out.println("java -jar ExhaustiveSearch.jar XmlFilePath");
            return;
        }
        double time = System.currentTimeMillis();
        String fileName = args[0];
        FloorplanExhaustive search = new FloorplanExhaustive(fileName);
        search.run();
        try {
            search.cfg.xmlFilePath = search.cfg.xmlFilePath.replaceAll(".xml", "_exse.xml");
            search.cfg.save();
        } catch (IOException ex) {
            Logger.getLogger(FloorplanExhaustive.class.getName()).log(Level.SEVERE, null, ex);
        }
        time = System.currentTimeMillis() - time;
        System.out.println("Time: " + time/1000.0 + " seconds.");
    }
}
