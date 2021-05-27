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

/**
 *
 * @author jlrisco
 */
public class Component {
    public static final int TYPE_CORE = 0;
    public static final int TYPE_MEMORY = 1;
    public static final int TYPE_REGISTER = 2;
    public static final int TYPE_CROSSBAR = 3;
    public static final int TYPE_THERMAL_VIA = 10;
    public static final int TYPE_OTHER = 20;

    protected int id;
    public int getId() { return id; }
    protected String name;
    protected int type;
    protected int x;
    protected int xMin;
    protected int xMax;
    protected int y;
    protected int yMin;
    protected int yMax;
    protected int z;
    protected int zMin;
    protected int zMax;
    protected int l;
    protected int w;
    protected int h;
    protected double[] dps; // Power densities

    public Component(int id, String name, int type, int x, int xMin, int xMax, int y, int yMin, int yMax, int z, int zMin, int zMax, int l, int w, int h, double[] dps) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.xMin = xMin;
        this.xMax = xMax;
        this.y = y;
        this.yMin = yMin;
        this.yMax = yMax;
        this.z = z;
        this.zMin = zMin;
        this.zMax = zMax;
        this.l = l;
        this.w = w;
        this.h = h;
        this.dps = dps;
    }

    public Component(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Component(String validLineOfFile, int cellSize) {
        validLineOfFile = validLineOfFile.replaceAll("\t", " ");
        validLineOfFile = validLineOfFile.replaceAll("  ", " ");
        String[] parts = validLineOfFile.split(" ");
        int i = 0;
        id = Integer.valueOf(parts[i++]);
        name = parts[i++];
        if(name.startsWith("Cross"))
            type = Component.TYPE_CROSSBAR;
        else if(name.startsWith("L"))
            type = Component.TYPE_MEMORY;
        else if(name.startsWith("C"))
            type = Component.TYPE_CORE;
        else
            type = Component.TYPE_OTHER;
        z = Integer.valueOf(parts[i++]);
        x = Integer.valueOf(parts[i++]) / cellSize;
        y = Integer.valueOf(parts[i++]) / cellSize;
        l = Integer.valueOf(parts[i++]) / cellSize;
        w = Integer.valueOf(parts[i++]) / cellSize;
        h = 1;
        dps = new double[1];
        dps[0] = Double.valueOf(parts[i++]) / (l * cellSize * Math.pow(10, -6) * w * cellSize * Math.pow(10, -6));
    }

    @Override
    public String toString() {
        if (id >= 0) {
            return String.valueOf(id);
        } else {
            return name;
        }
    }

    @Override
    public boolean equals(Object obj) {
        Component right = (Component) obj;
        if (right == null) {
            return false;
        }
        return id == right.id && name.equals(right.name);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.id;
        hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public Component clone() {
        Component clone = new Component(this.id, this.name, this.type, this.x, this.xMin, this.xMax, this.y, this.yMin, this.yMax, this.z, this.zMin, this.zMax, this.l, this.w, this.h, this.dps);
        return clone;
    }

    /*public double computeAveragePowerDensity() {
        double dpAvg = 0.0;
        for (int i = 0; i < dps.length; ++i) {
            dpAvg += dps[i];
        }
        dpAvg /= dps.length;
        return dpAvg;
    }*/

    /*public static Component findComponentById(int id, ArrayList<Component> components) {
    for (Component component : components) {
    if (component.id == id) {
    return component;
    }
    }
    return null;
    }*/
}
