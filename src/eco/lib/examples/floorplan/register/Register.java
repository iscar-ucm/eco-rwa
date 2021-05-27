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

/**
 *
 * @author jlrisco
 */
public class Register {
    protected int id;
    protected String name;
    protected int x;
    protected int xMin;
    protected int xMax;
    protected int y;
    protected int yMin;
    protected int yMax;
    protected int l;
    protected int w;
    protected double[] dps; // Power densities

    public Register(int id, String name, int x, int xMin, int xMax, int y, int yMin, int yMax, int l, int w, double[] dps) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.xMin = xMin;
        this.xMax = xMax;
        this.y = y;
        this.yMin = yMin;
        this.yMax = yMax;
        this.l = l;
        this.w = w;
        this.dps = dps;
    }

    public Register(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        if (id >= 0) {
            return "[" + name + "]";
        } else {
            return name;
        }
    }

    @Override
    public Register clone() {
        Register clone = new Register(this.id, this.name, this.x, this.xMin, this.xMax, this.y, this.yMin, this.yMax, this.l, this.w, this.dps);
        return clone;
    }
}
