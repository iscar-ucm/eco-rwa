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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author jlrisco
 */
public class FloorplanConfiguration {

    public static final String VERSION = "5.0";
    protected String xmlFilePath;
    protected String xmlVersion = FloorplanConfiguration.VERSION;
    protected int cellSizeInMicroMeters, maxLengthInCells, maxWidthInCells, numLayers, numPowerProfiles;
    protected int maxID = Integer.MIN_VALUE;
    protected double maxDP = Double.MIN_VALUE;
    protected HashMap<Integer, Component> components;

    public HashMap<Integer, Component> getComponents() {
        return components;
    }
    protected HashMap<Integer, HashSet<Integer>> couplings;
    protected LinkedList<ThermalVia> thermalVias;

    public FloorplanConfiguration(String xmlFilePath) {
        this.xmlFilePath = xmlFilePath;
        components = new HashMap<Integer, Component>();
        couplings = new HashMap<>();
        thermalVias = new LinkedList<ThermalVia>();
        try {
            load();
            for (Component c : components.values()) {
                for (int i = 0; i < c.dps.length; ++i) {
                    if (c.dps[i] > maxDP) {
                        maxDP = c.dps[i];
                    }
                    if (c.id > maxID) {
                        maxID = c.id;
                    }
                }
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(FloorplanConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FloorplanConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(FloorplanConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public FloorplanConfiguration(String xmlFilePath, int cellSizeInMicroMeters, int maxLengthInCells, int maxWidthInCells, int maxHeight, int numPowerProfiles, HashMap<Integer, Component> components, HashMap<Integer, HashSet<Integer>> couplings, LinkedList<ThermalVia> thermalVias) {
        this.xmlFilePath = xmlFilePath;
        this.cellSizeInMicroMeters = cellSizeInMicroMeters;
        this.maxLengthInCells = maxLengthInCells;
        this.maxWidthInCells = maxWidthInCells;
        this.numLayers = maxHeight;
        this.numPowerProfiles = numPowerProfiles;
        this.components = components;
        for (Component c : components.values()) {
            for (int i = 0; i < c.dps.length; ++i) {
                if (c.dps[i] > maxDP) {
                    maxDP = c.dps[i];
                }
            }
        }
        this.couplings = couplings;
        this.thermalVias = thermalVias;
    }

    @Override
    public FloorplanConfiguration clone() {
        FloorplanConfiguration clone = new FloorplanConfiguration(this.xmlFilePath);
        return clone;
    }

    private void load() throws ParserConfigurationException, IOException, SAXException {
        Logger.getLogger(FloorplanConfiguration.class.getName()).fine("Loading " + xmlFilePath + "...");
        components.clear();
        couplings.clear();
        thermalVias.clear();
        File file = new File(xmlFilePath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xmlDoc = builder.parse(file.toURI().toString());
        Element xmlRoot = xmlDoc.getDocumentElement();
        xmlVersion = xmlRoot.getAttribute("Version");
        cellSizeInMicroMeters = Integer.valueOf(xmlRoot.getAttribute("CellSize"));
        maxLengthInCells = Integer.valueOf(xmlRoot.getAttribute("Length"));
        maxWidthInCells = Integer.valueOf(xmlRoot.getAttribute("Width"));
        numLayers = Integer.valueOf(xmlRoot.getAttribute("NumLayers"));
        numPowerProfiles = Integer.valueOf(xmlRoot.getAttribute("NumPowerProfiles"));

        NodeList xmlBlockList = xmlDoc.getElementsByTagName("Block");
        for (int i = 0; i < xmlBlockList.getLength(); ++i) {
            Element xmlBlock = (Element) xmlBlockList.item(i);
            int id = Integer.valueOf(xmlBlock.getAttribute("id"));
            String name = xmlBlock.getAttribute("name");
            int type = Double.valueOf(xmlBlock.getAttribute("type")).intValue();
            int x = Double.valueOf(xmlBlock.getAttribute("x")).intValue();
            int xMin = Double.valueOf(xmlBlock.getAttribute("xMin")).intValue();
            int xMax = Double.valueOf(xmlBlock.getAttribute("xMax")).intValue();
            int y = Double.valueOf(xmlBlock.getAttribute("y")).intValue();
            int yMin = Double.valueOf(xmlBlock.getAttribute("yMin")).intValue();
            int yMax = Double.valueOf(xmlBlock.getAttribute("yMax")).intValue();
            int z = Double.valueOf(xmlBlock.getAttribute("z")).intValue();
            int zMin = Double.valueOf(xmlBlock.getAttribute("zMin")).intValue();
            int zMax = Double.valueOf(xmlBlock.getAttribute("zMax")).intValue();
            int l = Double.valueOf(xmlBlock.getAttribute("l")).intValue();
            int w = Double.valueOf(xmlBlock.getAttribute("w")).intValue();
            int h = Double.valueOf(xmlBlock.getAttribute("h")).intValue();
            double[] dps = new double[numPowerProfiles];
            for (int j = 0; j < numPowerProfiles; ++j) {
                dps[j] = Double.valueOf(xmlBlock.getAttribute("dp" + j)).doubleValue();
            }
            Component component = new Component(id, name, type, x, xMin, xMax, y, yMin, yMax, z, zMin, zMax, l, w, h, dps);
            components.put(id, component);
        }

        NodeList xmlCouplingList = xmlDoc.getElementsByTagName("Coupling");
        for (int i = 0; i < xmlCouplingList.getLength(); ++i) {
            Element xmlCoupling = (Element) xmlCouplingList.item(i);
            int idFrom = Integer.valueOf(xmlCoupling.getAttribute("idFrom"));
            int idTo = Integer.valueOf(xmlCoupling.getAttribute("idTo"));
            HashSet<Integer> idsTo = couplings.get(idFrom);
            if (idsTo == null) {
                couplings.put(idFrom, new HashSet<Integer>());
            }
            couplings.get(idFrom).add(idTo);
        }

        NodeList xmlThermalViaList = xmlDoc.getElementsByTagName("ThermalVia");
        for (int i = 0; i < xmlThermalViaList.getLength(); ++i) {
            Element xmlThermalVia = (Element) xmlThermalViaList.item(i);
            int zIni = Integer.valueOf(xmlThermalVia.getAttribute("zIni"));
            int zEnd = Integer.valueOf(xmlThermalVia.getAttribute("zEnd"));
            int x = Integer.valueOf(xmlThermalVia.getAttribute("x"));
            int y = Integer.valueOf(xmlThermalVia.getAttribute("y"));
            ThermalVia thermalVia = new ThermalVia(zIni, zEnd, x, y);
            thermalVias.add(thermalVia);
        }

        Logger.getLogger(FloorplanConfiguration.class.getName()).fine("done.");
    }

    public double[] computeTempObj() {
        double[] result = new double[numPowerProfiles];
        for (int i = 0; i < result.length; ++i) {
            result[i] = 0.0;
        }
        double dist = 0;
        Component[] componentsAsArray = components.values().toArray(new Component[components.size()]);
        for (int i = 0; i < componentsAsArray.length - 1; ++i) {
            Component cI = componentsAsArray[i];
            for (int j = i + 1; j < componentsAsArray.length; ++j) {
                Component cJ = componentsAsArray[j];
                dist = Math.sqrt(Math.pow(cI.x + cI.l / 2.0 - cJ.x - cJ.l / 2.0, 2) + Math.pow(cI.y + cI.w / 2.0 - cJ.y - cJ.w / 2.0, 2) + Math.pow(cI.z + cI.h / 2.0 - cJ.z - cJ.h / 2.0, 2));
                for (int p = 0; p < numPowerProfiles; ++p) {
                    result[p] += ((cI.dps[p] / maxDP) * (cJ.dps[p] / maxDP)) / dist;
                }
            }
        }
        return result;
    }

    public double computeWireObj() {
        double result = 0;
        Iterator<Integer> itrFrom = couplings.keySet().iterator();
        while (itrFrom.hasNext()) {
            int idFrom = itrFrom.next();
            Component cFrom = components.get(idFrom);
            Iterator<Integer> itrTo = couplings.get(idFrom).iterator();
            while (itrTo.hasNext()) {
                int idTo = itrTo.next();
                Component cTo = components.get(idTo);
                result += Math.abs(cFrom.x + cFrom.l / 2 - cTo.x - cTo.l / 2) + Math.abs(cFrom.y + cFrom.w / 2 - cTo.y - cTo.w / 2) + Math.abs(cFrom.z - cTo.z);
            }
        }
        return result;
    }

    private double findBestTSV(Component cI, Component cJ) {
        double dist, distMin = Double.POSITIVE_INFINITY;
        boolean found = false;
        int zMin = Math.min(cI.z, cJ.z);
        // I must find the starting index in the chromosome:
        boolean isTSV = false;
        for (ThermalVia thermalVia : thermalVias) {
            isTSV = thermalVia.zEnd <= zMin;
            if (isTSV) {
                dist = Math.min(Math.abs(cI.x - thermalVia.x), Math.abs(cI.x + cI.l - thermalVia.x))
                        + Math.min(Math.abs(cI.y - thermalVia.y), Math.abs(cI.y + cI.w - thermalVia.y))
                        + Math.abs((cI.z - cJ.z))
                        + Math.min(Math.abs(cJ.x - thermalVia.x), Math.abs(cJ.x + cJ.l - thermalVia.x))
                        + Math.min(Math.abs(cJ.y - thermalVia.y), Math.abs(cJ.y + cJ.w - thermalVia.y));
                if (dist < distMin) {
                    distMin = dist;
                }
                found = true;
            }
        }
        return (found) ? distMin : -1;
    }

    public double computeWire(boolean withTSVs) {
        double result = 0;
        int xLI, xRI, xLJ, xRJ, yUI, yDI, yUJ, yDJ, dx, dy, dz;
        Iterator<Integer> itrFrom = couplings.keySet().iterator();
        while (itrFrom.hasNext()) {
            int idFrom = itrFrom.next();
            Component cI = components.get(idFrom);
            xLI = cI.x;
            xRI = cI.x + cI.l;
            yUI = cI.y;
            yDI = cI.y + cI.w;
            Iterator<Integer> itrTo = couplings.get(idFrom).iterator();
            while (itrTo.hasNext()) {
                int idTo = itrTo.next();
                Component cJ = components.get(idTo);
                xLJ = cJ.x;
                xRJ = cJ.x + cJ.l;
                yUJ = cJ.y;
                yDJ = cJ.y + cJ.w;
                dz = Math.abs(cI.z - cJ.z);
                if ((yUI >= yUJ && yUI <= yDJ) || (yDI >= yUJ && yDI <= yDJ)) {
                    dy = 0;
                } else {
                    dy = Math.min(Math.abs(yUI - yUJ), Math.abs(yUI - yDJ));
                    dy = Math.min(Math.abs(yDI - yUJ), dy);
                    dy = Math.min(Math.abs(yDI - yDJ), dy);
                }
                if (dz > 0) {
                    double resultTemp = this.findBestTSV(cI, cJ);
                    if (!withTSVs || resultTemp < 0) {
                        // Cableamos siempre al mismo borde del chip (es escalonado por un borde, ver chip CMOSAIC)
                        result += dz + cI.x + cJ.x + dy;
                    } else {
                        result += resultTemp;
                    }
                } else {
                    if ((xLI >= xLJ && xLI <= xRJ) || (xRI >= xLJ && xRI <= xRJ)) {
                        dx = 0;
                    } else {
                        dx = Math.min(Math.abs(xLI - xLJ), Math.abs(xLI - xRJ));
                        dx = Math.min(Math.abs(xRI - xLJ), dx);
                        dx = Math.min(Math.abs(xRI - xRJ), dx);
                    }
                    result += dx + dy;
                }
            }
        }
        return result;
    }

    public double computeWireObjWithoutThermalVias() {
        double result = 0;
        Iterator<Integer> itrFrom = couplings.keySet().iterator();
        while (itrFrom.hasNext()) {
            int idFrom = itrFrom.next();
            Component cFrom = components.get(idFrom);
            Iterator<Integer> itrTo = couplings.get(idFrom).iterator();
            while (itrTo.hasNext()) {
                int idTo = itrTo.next();
                Component cTo = components.get(idTo);
                result += Math.abs(cFrom.x + cFrom.l / 2 - cTo.x - cTo.l / 2) + Math.abs(cFrom.y + cFrom.w / 2 - cTo.y - cTo.w / 2) + Math.abs(cFrom.z - cTo.z);
                if (cFrom.z != cTo.z) {
                    // A lo anterior hay que añadirle la distancia a las paredes.
                    // Aproximamos al más lejano, para no andar sumando periferias
                    double maxCompFrom = Math.max(cFrom.x + cFrom.l / 2.0, this.maxLengthInCells - cFrom.x - cFrom.l / 2.0);
                    maxCompFrom = Math.max(maxCompFrom, cFrom.y + cFrom.w / 2.0);
                    maxCompFrom = Math.max(maxCompFrom, this.maxWidthInCells - cFrom.y - cFrom.w / 2.0);
                    double maxCompTo = Math.max(cTo.x + cTo.l / 2.0, this.maxLengthInCells - cTo.x - cTo.l / 2.0);
                    maxCompTo = Math.max(maxCompTo, cTo.y + cTo.w / 2.0);
                    maxCompTo = Math.max(maxCompTo, this.maxWidthInCells - cTo.y - cTo.w / 2.0);
                    result += (maxCompFrom + maxCompTo);
                }
            }
        }
        return result;
    }

    public void save() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(xmlFilePath)));
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");

        writer.write("<Floorplan Version=\"" + FloorplanConfiguration.VERSION + "\" CellSize=\"" + cellSizeInMicroMeters + "\" Length=\"" + maxLengthInCells + "\" Width=\"" + maxWidthInCells + "\" NumLayers=\"" + numLayers + "\" NumPowerProfiles=\"" + numPowerProfiles + "\">\n");

        writer.write("\t<Blocks>\n");
        for (Component component : components.values()) {
            writer.write("\t\t<Block id=\"" + component.id + "\" name=\"" + component.name + "\" type=\"" + component.type + "\" xMin=\"" + component.xMin + "\" x=\"" + component.x + "\" xMax=\"" + component.xMax + "\" yMin=\"" + component.yMin + "\" y=\"" + component.y + "\" yMax=\"" + component.yMax + "\" zMin=\"" + component.zMin + "\" z=\"" + component.z + "\" zMax=\"" + component.zMax + "\" l=\"" + component.l + "\" w=\"" + component.w + "\" h=\"" + component.h + "\"");
            for (int i = 0; i < numPowerProfiles; ++i) {
                writer.write(" dp" + i + "=\"" + component.dps[i] + "\"");
            }
            writer.write("/>\n");
        }

        writer.write("\t</Blocks>\n");
        writer.write("\t<Couplings>\n");
        Iterator<Integer> itrFrom = couplings.keySet().iterator();
        while (itrFrom.hasNext()) {
            int idFrom = itrFrom.next();
            Component compFrom = components.get(idFrom);
            Iterator<Integer> itrTo = couplings.get(idFrom).iterator();
            while (itrTo.hasNext()) {
                int idTo = itrTo.next();
                Component compTo = components.get(idTo);
                writer.write("\t\t<Coupling idFrom=\"" + compFrom.id + "\" idTo=\"" + compTo.id + "\"/>\n");
            }
        }
        writer.write("\t</Couplings>\n");
        writer.write("\t<ThermalVias>\n");
        for (ThermalVia thermalVia : thermalVias) {
            writer.write("\t\t<ThermalVia zIni=\"" + thermalVia.zIni + "\" zEnd=\"" + thermalVia.zEnd + "\" x=\"" + thermalVia.x + "\" y=\"" + thermalVia.y + "\"/>\n");
        }
        writer.write("\t</ThermalVias>\n");
        writer.write("</Floorplan>\n");
        writer.flush();
        writer.close();
    }

    public void fixLimits() {
        for (Component c : components.values()) {
            c.xMin = 0;
            c.xMax = this.maxLengthInCells - c.l;
            c.yMin = 0;
            c.yMax = this.maxWidthInCells - c.w;
            c.zMin = 0;
            c.zMax = this.numLayers - 1;
        }
    }

    public static void main(String[] args) {
        String scenarioPath = "D:\\jlrisco\\Trabajo\\Investiga\\Estudiantes\\DavidCuesta\\benchmarks\\2011_LATW\\NiagaraHeteroC64L5_10_10_163.284920692033_4302.68826266009.xml";
        FloorplanConfiguration cfg = new FloorplanConfiguration(scenarioPath);
        System.out.println(cfg.computeWireObj());
    }
}
