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
package eco.lib.examples.floorplan.register;

import java.util.Comparator;

/**
 *
 * @author jlrisco
 */
public class RegisterThermalComparator implements Comparator<Register> {

    public int compare(Register o1, Register o2) {
        double[] z1 = o1.dps;
        double[] z2 = o2.dps;
        int n = Math.min(z1.length, z2.length);

        boolean worst = false;
        boolean better = false;
        boolean indiff = false;
        for (int i = 0; !(indiff) && i < n; i++) {
            if (z1[i] < z2[i]) {
                worst = true;
            }
            if (z1[i] > z2[i]) {
                better = true;
            }
            indiff = (worst && better);
        }

        if (better && !worst) {
            return -1;
        } else if (worst && !better) {
            return 1;
        }
        return 0;
    }
}
