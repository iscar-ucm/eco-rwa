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

import hero.lib.examples.floorplan.FloorplanTsv;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
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
public class Manager {

    public static final int XML31_TO_50 = 1;
    public static final int XML40_TO_50 = 2;
    public static final int DCUESTA2XML = 3;
    public static final int XML_STATS = 4;
    public static final int XML2OPLDAT1 = 5;
    public static final int XML2OPLDAT2 = 6;
    public static final int FIX_LIMITS = 7;

    public static void update3150(String pathToXmlFile) throws ParserConfigurationException, SAXException, IOException {
        HashMap<Integer, Component> components = new HashMap<Integer, Component>();
        HashMap<Integer, HashSet<Integer>> couplings = new HashMap<Integer, HashSet<Integer>>();

        File file = new File(pathToXmlFile);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xmlDoc = builder.parse(file.toURI().toString());
        Element xmlRoot = xmlDoc.getDocumentElement();
        int cellSizeInMicroMeters = Integer.valueOf(xmlRoot.getAttribute("CellSize"));
        int maxLengthInCells = Integer.valueOf(xmlRoot.getAttribute("L"));
        int maxWidthInCells = Integer.valueOf(xmlRoot.getAttribute("W"));
        int numLayers = Integer.valueOf(xmlRoot.getAttribute("H"));

        NodeList xmlBlockList = xmlDoc.getElementsByTagName("Block");
        for (int i = 0; i < xmlBlockList.getLength(); ++i) {
            Element xmlBlock = (Element) xmlBlockList.item(i);
            int id = Integer.valueOf(xmlBlock.getAttribute("id"));
            String name = xmlBlock.getAttribute("name");
            int type = Integer.valueOf(xmlBlock.getAttribute("type"));
            if (type == 0) {
                type = Component.TYPE_CORE;
            } else {
                type = Component.TYPE_MEMORY;
            }
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
            double p = Double.valueOf(xmlBlock.getAttribute("p"));
            double[] dps = new double[]{p / (l * cellSizeInMicroMeters * Math.pow(10, -6) * w * cellSizeInMicroMeters * Math.pow(10, -6))};
            Component component = new Component(id, name, type, x, xMin, xMax, y, yMin, yMax, z, zMin, zMax, l, w, h, dps);
            components.put(id, component);
        }

        NodeList xmlCouplingList = xmlDoc.getElementsByTagName("Coupling");
        for (int i = 0; i < xmlCouplingList.getLength(); ++i) {
            Element xmlCoupling = (Element) xmlCouplingList.item(i);
            int idFrom = Integer.valueOf(xmlCoupling.getAttribute("idFrom"));
            HashSet<Integer> idsTo = couplings.get(idFrom);
            if (idsTo == null) {
                couplings.put(idFrom, new HashSet<Integer>());
            }
            int idTo = Integer.valueOf(xmlCoupling.getAttribute("idTo"));
            couplings.get(idFrom).add(idTo);
        }
        FloorplanConfiguration cfg = new FloorplanConfiguration(pathToXmlFile.replaceAll(".xml", "_" + FloorplanConfiguration.VERSION + ".xml"), cellSizeInMicroMeters, maxLengthInCells, maxWidthInCells, numLayers, 1, components, couplings, new LinkedList<ThermalVia>());
        cfg.save();

    }

    public static void update4050(String pathToXmlFile) throws IOException, ParserConfigurationException, SAXException {
        HashMap<Integer, Component> components = new HashMap<Integer, Component>();
        HashMap<Integer, HashSet<Integer>> couplings = new HashMap<Integer, HashSet<Integer>>();
        LinkedList<ThermalVia> thermalVias = new LinkedList<ThermalVia>();

        File file = new File(pathToXmlFile);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xmlDoc = builder.parse(file.toURI().toString());
        Element xmlRoot = xmlDoc.getDocumentElement();
        int cellSizeInMicroMeters = Integer.valueOf(xmlRoot.getAttribute("CellSize"));
        int maxLengthInCells = Integer.valueOf(xmlRoot.getAttribute("L"));
        int maxWidthInCells = Integer.valueOf(xmlRoot.getAttribute("W"));
        int numLayers = Integer.valueOf(xmlRoot.getAttribute("H"));

        int maxID = Integer.MIN_VALUE;

        NodeList xmlBlockList = xmlDoc.getElementsByTagName("Block");
        for (int i = 0; i < xmlBlockList.getLength(); ++i) {
            Element xmlBlock = (Element) xmlBlockList.item(i);
            int id = Integer.valueOf(xmlBlock.getAttribute("id"));
            if (maxID < id) {
                maxID = id;
            }
            String name = xmlBlock.getAttribute("name");
            int type = Integer.valueOf(xmlBlock.getAttribute("type"));
            if (type == 0) {
                type = Component.TYPE_CORE;
            } else {
                type = Component.TYPE_MEMORY;
            }
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
            double dp = Double.valueOf(xmlBlock.getAttribute("dp"));
            double[] dps = new double[]{dp};
            Component component = new Component(id, name, type, x, xMin, xMax, y, yMin, yMax, z, zMin, zMax, l, w, h, dps);
            components.put(id, component);
        }

        NodeList xmlCouplingList = xmlDoc.getElementsByTagName("Coupling");
        for (int i = 0; i < xmlCouplingList.getLength(); ++i) {
            Element xmlCoupling = (Element) xmlCouplingList.item(i);
            int idFrom = Integer.valueOf(xmlCoupling.getAttribute("idFrom"));
            HashSet<Integer> idsTo = couplings.get(idFrom);
            if (idsTo == null) {
                couplings.put(idFrom, new HashSet<Integer>());
            }
            int idTo = Integer.valueOf(xmlCoupling.getAttribute("idTo"));
            couplings.get(idFrom).add(idTo);
        }

        NodeList xmlThermalViaList = xmlDoc.getElementsByTagName("ThermalVia");
        for (int i = 0; i < xmlThermalViaList.getLength(); ++i) {
            Element xmlThermal = (Element) xmlThermalViaList.item(i);
            int zIni = Integer.valueOf(xmlThermal.getAttribute("zIni"));
            int zEnd = Integer.valueOf(xmlThermal.getAttribute("zEnd"));
            int x = Double.valueOf(xmlThermal.getAttribute("x")).intValue();
            int y = Double.valueOf(xmlThermal.getAttribute("y")).intValue();
            ThermalVia thermalVia = new ThermalVia(zIni, zEnd, x, y);
            thermalVias.add(thermalVia);
        }

        FloorplanConfiguration cfg = new FloorplanConfiguration(pathToXmlFile.replaceAll(".xml", "_" + FloorplanConfiguration.VERSION + ".xml"), cellSizeInMicroMeters, maxLengthInCells, maxWidthInCells, numLayers, 1, components, couplings, thermalVias);
        cfg.save();

    }

    public static void fromTxtToXml(String pathToBlocksFile, String pathToCouplingsFile, int cellSizeInMicroMeters, int maxLengthInCells, int maxWidthInCells, int numLayers, String pathToXmlFile) throws FileNotFoundException, IOException {
        HashMap<Integer, Component> components = new HashMap<Integer, Component>();
        BufferedReader reader = new BufferedReader(new FileReader(new File(pathToBlocksFile)));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("#") && line.length() > 0) {
                Component component = new Component(line, cellSizeInMicroMeters);
                component.xMin = 0;
                component.xMax = maxLengthInCells - component.l;
                component.yMin = 0;
                component.yMax = maxWidthInCells - component.w;
                component.zMin = 0;
                component.zMax = numLayers - component.h;
                components.put(component.id, component);
            }
        }
        reader.close();

        HashMap<Integer, HashSet<Integer>> couplings = new HashMap<Integer, HashSet<Integer>>();
        int idFrom, idTo;
        reader = new BufferedReader(new FileReader(new File(pathToCouplingsFile)));
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("#") && line.length() > 0) {
                line = line.replaceAll("\t", " ");
                line = line.replaceAll("  ", " ");
                String[] parts = line.split(" ");
                idFrom = Integer.valueOf(parts[0]);
                idTo = Integer.valueOf(parts[1]);
                HashSet<Integer> idsTo = couplings.get(idFrom);
                if (idsTo == null) {
                    couplings.put(idFrom, new HashSet<Integer>());
                }
                couplings.get(idFrom).add(idTo);
            }
        }
        FloorplanConfiguration cfg = new FloorplanConfiguration(pathToXmlFile, cellSizeInMicroMeters, maxLengthInCells, maxWidthInCells, numLayers, 1, components, couplings, new LinkedList<ThermalVia>());
        cfg.save();

    }

    public static double computeWireObj(FloorplanConfiguration cfg) {
        HashMap<Integer, HashSet<Integer>> couplings = cfg.couplings;
        HashMap<Integer, Component> components = cfg.components;
        double result = 0;
        int xL1, xR1, xL2, xR2, yU1, yD1, yU2, yD2, dx, dy;
        Iterator<Integer> itrFrom = couplings.keySet().iterator();
        while (itrFrom.hasNext()) {
            int idFrom = itrFrom.next();
            Component cFrom = components.get(idFrom);
            xL1 = cFrom.x;
            xR1 = cFrom.x + cFrom.l;
            yU1 = cFrom.y;
            yD1 = cFrom.y + cFrom.w;
            Iterator<Integer> itrTo = couplings.get(idFrom).iterator();
            while (itrTo.hasNext()) {
                int idTo = itrTo.next();
                Component cTo = components.get(idTo);
                xL2 = cTo.x;
                xR2 = cTo.x + cTo.l;
                yU2 = cTo.y;
                yD2 = cTo.y + cTo.w;
                if ((xL1 >= xL2 && xL1 <= xR2) || (xR1 >= xL2 && xR1 <= xR2)) {
                    dx = 0;
                } else {
                    dx = Math.min(Math.abs(xL1 - xL2), Math.abs(xL1 - xR2));
                    dx = Math.min(Math.abs(xR1 - xL2), dx);
                    dx = Math.min(Math.abs(xR1 - xR2), dx);
                }
                if ((yU1 >= yU2 && yU1 <= yD2) || (yD1 >= yU2 && yD1 <= yD2)) {
                    dy = 0;
                } else {
                    dy = Math.min(Math.abs(yU1 - yU2), Math.abs(yU1 - yD2));
                    dy = Math.min(Math.abs(yD1 - yU2), dy);
                    dy = Math.min(Math.abs(yD1 - yD2), dy);
                }
                result += dx + dy + Math.abs(cFrom.z - cTo.z);
            }
        }
        return result;
    }

    public static void printStats(String pathToXmlFile) {
        FloorplanConfiguration cfg = new FloorplanConfiguration(pathToXmlFile);
        int totalArea = cfg.maxLengthInCells * cfg.maxWidthInCells;
        int areaComponents = 0;
        for (Component c : cfg.components.values()) {
            areaComponents += c.l * c.w;
        }

        double wireObjWithThermalVias = cfg.computeWire(true);
        double wireObjWithoutThermalVias = cfg.computeWire(false);
        double[] tempObjs = cfg.computeTempObj();
        System.out.println("File: " + pathToXmlFile);
        System.out.println("Total design area in cells (per layer): " + totalArea);
        System.out.println("Total components area in cells: " + areaComponents);
        System.out.println("Number of layers needed: " + Math.ceil((1.0 * areaComponents) / (1.0 * totalArea)));
        System.out.println("Manhattan Wire Length W/O TSVs (in mm): " + Manager.computeWireObj(cfg) * cfg.cellSizeInMicroMeters * 1.0e-3);
        System.out.println("Wire Length W/O TSVs (in mm): " + wireObjWithoutThermalVias * cfg.cellSizeInMicroMeters * 1.0e-3);
        System.out.println("Wire Length W TSVs (in mm): " + wireObjWithThermalVias * cfg.cellSizeInMicroMeters * 1.0e-3);
        System.out.println("Number of power profiles: " + cfg.numPowerProfiles);
        System.out.println("Wire/Temperatures/profile: ");
        System.out.print(cfg.computeWireObj());
        for (int i = 0; i < tempObjs.length; ++i) {
            System.out.print(", ");
            System.out.print(tempObjs[i]);
        }
        System.out.println("");
    }

    public static void fromXmlToDat(String pathToXmlFile, int powerProfile, String pathToDatFile) throws IOException {
        FloorplanConfiguration cfg = new FloorplanConfiguration(pathToXmlFile);

        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(pathToDatFile)));
        String doubleLine = "//======================================================\n";
        writer.write(doubleLine);
        writer.write("// " + pathToXmlFile + "\n");
        writer.write(doubleLine);
        writer.write("CellSize = " + cfg.cellSizeInMicroMeters + "; // Tamaño de la celda\n\n");
        writer.write("L = " + cfg.maxLengthInCells + "; // Anchura máxima\n\n");
        writer.write("W = " + cfg.maxWidthInCells + "; // Profundidad máxima\n\n");
        writer.write("H = " + cfg.numLayers + "; // Altura máxima\n\n");

        writer.write("// Módulos\n");
        writer.write("Modules = {");
        for (Component component : cfg.components.values()) {
            writer.write(" <");
            writer.write("" + component.id);
            writer.write(" \"" + component.name + "\"");
            writer.write(" " + component.type);
            writer.write(" " + -1);
            writer.write(" " + 0); // xMin
            writer.write(" " + (cfg.maxLengthInCells - component.l)); // xMax
            writer.write(" " + 0); // yMin
            writer.write(" " + (cfg.maxWidthInCells - component.w)); // yMax
            writer.write(" " + 0); // zMin
            writer.write(" " + (cfg.numLayers - component.h)); // zMax
            writer.write(" " + component.l);
            writer.write(" " + component.w);
            writer.write(" " + component.h);
            writer.write(" " + component.dps[powerProfile]);
            writer.write(">");
        }
        writer.write("};\n\n");

        writer.write("// Cableado entre módulos\n");
        writer.write("Wires = {");
        Set<Integer> idsFrom = cfg.couplings.keySet();
        for (Integer idFrom : idsFrom) {
            HashSet<Integer> idsTo = cfg.couplings.get(idFrom);
            for (Integer idTo : idsTo) {
                writer.write(" <");
                writer.write("" + idFrom);
                writer.write(" " + idTo);
                writer.write(">");
            }
        }
        writer.write("};\n\n");
        writer.flush();
        writer.close();
    }

    public static void fromXmlToDatTsvs(String pathToXmlFile, int powerProfile, String pathToDatFile) throws IOException {
        StringBuilder stringIds = new StringBuilder();
        StringBuilder stringData = new StringBuilder();

        FloorplanConfiguration cfg = new FloorplanConfiguration(pathToXmlFile);

        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(pathToDatFile)));
        String doubleLine = "//======================================================\n";
        writer.write(doubleLine);
        writer.write("// " + pathToXmlFile + "\n");
        writer.write(doubleLine);
        writer.write("CellSize = " + cfg.cellSizeInMicroMeters + "; // Tamaño de la celda\n\n");
        writer.write("L = " + cfg.maxLengthInCells + "; // Anchura máxima\n\n");
        writer.write("W = " + cfg.maxWidthInCells + "; // Profundidad máxima\n\n");
        writer.write("H = " + cfg.numLayers + "; // Altura máxima\n\n");

        Set<Integer> compIds = cfg.components.keySet();
        writer.write("// Módulos\n");
        writer.write("ModuleIds = {");
        for (Integer compId : compIds) {
            writer.write(" " + compId);
        }
        writer.write("};\n");
        writer.write("Modules = #[");
        for (Integer compId : compIds) {
            Component component = cfg.components.get(compId);
            writer.write(" " + compId + ": #<name:\"" + component.name + "\" type:" + component.type + " turn:" + -1 + " x:" + component.x + " y:" + component.y + " z:" + component.z + " l:" + component.l + " w:" + component.w + " h:" + component.h + " dp:" + component.dps[powerProfile] + ">#\n");
        }
        writer.write("]#;\n\n");

        FloorplanTsv tsvs = new FloorplanTsv(cfg);
        int idVia = 1;
        HashMap<Integer, HashMap<Integer, Point>> thermalVias = new HashMap<Integer, HashMap<Integer, Point>>();
        stringIds.append("// Thermal vias\n");
        stringIds.append("ThermalIds = {");
        stringData.append("ThermalVias = #[");
        for (int i = 0; i < cfg.numLayers - 1; ++i) {
            thermalVias.put(i, new HashMap<Integer, Point>());
            LinkedList<Point> tsvPoints = tsvs.getAllowedThermalViasToLevel(i);
            for (Point point : tsvPoints) {
                stringIds.append(" " + idVia);
                stringData.append(" " + idVia + ": #<level:" + i + " x:" + point.x + " y:" + point.y + ">#\n");
                thermalVias.get(i).put(idVia, point);
                idVia++;
            }
        }
        stringIds.append("};\n");
        stringData.append("]#;\n\n");
        writer.write(stringIds.toString());
        writer.write(stringData.toString());
        stringIds = new StringBuilder();
        stringData = new StringBuilder();

        Set<Integer> idsFrom = cfg.couplings.keySet();
        writer.write("// Cableado entre módulos\n");
        writer.write("WireIds = {");
        for (Integer idFrom : idsFrom) {
            HashSet<Integer> idsTo = cfg.couplings.get(idFrom);
            for (Integer idTo : idsTo) {
                writer.write(" <" + idFrom + " " + idTo + ">");
            }
        }
        writer.write("};\n");
        writer.write("Wires = #[");
        for (Integer idFrom : idsFrom) {
            Component cFrom = cfg.components.get(idFrom);
            HashSet<Integer> idsTo = cfg.couplings.get(idFrom);
            for (Integer idTo : idsTo) {
                Component cTo = cfg.components.get(idTo);
                writer.write(" <" + idFrom + " " + idTo + ">: ");
                writer.write("#<dx:" + Math.abs(cFrom.x - cTo.x) + " dy:" + Math.abs(cFrom.y - cTo.y) + " dz:" + Math.abs(cFrom.z - cTo.z) + ">#\n");
            }
        }
        writer.write("]#;\n\n");

        writer.write("// Distancias de thermal vías a componentes\n");
        stringIds.append("ThermalToCompIds = {");
        stringData.append("ThermalToComp = #[");
        for (Integer idFrom : idsFrom) {
            Component cFrom = cfg.components.get(idFrom);
            HashSet<Integer> idsTo = cfg.couplings.get(idFrom);
            for (Integer idTo : idsTo) {
                Component cTo = cfg.components.get(idTo);
                int minZ = Math.min(cFrom.z, cTo.z);
                if (minZ == cfg.numLayers - 1) {
                    continue;
                }
                for (int level = 0; level <= minZ; level++) {
                    HashMap<Integer, Point> tsvPoints = thermalVias.get(level);
                    Set<Integer> tsvIds = tsvPoints.keySet();
                    for (Integer tsvId : tsvIds) {
                        Point p = tsvPoints.get(tsvId);
                        stringIds.append(" <" + tsvId + " " + idTo + ">\n");
                        stringData.append(" <" + tsvId + " " + idTo + ">: #<dx:" + Math.abs(cTo.x - p.x) + " dy:" + Math.abs(cTo.y - p.y) + ">#\n");
                    }
                }
            }
        }
        stringIds.append("};\n");
        stringData.append("]#;\n\n");
        writer.write(stringIds.toString());
        writer.write(stringData.toString());
        stringIds = new StringBuilder();
        stringData = new StringBuilder();

        writer.write("// Distancias de componentes a thermal vias\n");
        stringIds.append("CompToThermalIds = {");
        stringData.append("CompToThermal = #[");
        for (Integer idFrom : idsFrom) {
            Component cFrom = cfg.components.get(idFrom);
            HashSet<Integer> idsTo = cfg.couplings.get(idFrom);
            for (Integer idTo : idsTo) {
                Component cTo = cfg.components.get(idTo);
                int minZ = Math.min(cFrom.z, cTo.z);
                if (minZ == cfg.numLayers - 1) {
                    continue;
                }
                for (int level = 0; level <= minZ; level++) {
                    HashMap<Integer, Point> tsvPoints = thermalVias.get(level);
                    Set<Integer> tsvIds = tsvPoints.keySet();
                    for (Integer tsvId : tsvIds) {
                        Point p = tsvPoints.get(tsvId);
                        stringIds.append(" <" + idFrom + " " + tsvId + ">\n");
                        stringData.append(" <" + idFrom + " " + tsvId + ">: #<dx:" + Math.abs(cFrom.x - p.x) + " dy:" + Math.abs(cFrom.y - p.y) + ">#\n");
                    }
                }
            }
        }
        stringIds.append("};\n");
        stringData.append("]#;\n\n");
        writer.write(stringIds.toString());
        writer.write(stringData.toString());
        stringIds = new StringBuilder();
        stringData = new StringBuilder();

        writer.flush();
        writer.close();
    }

    public static void fixLimits(String pathToXmlFile, String pathToNewXmlFile) {
        FloorplanConfiguration cfg = new FloorplanConfiguration(pathToXmlFile);
        cfg.fixLimits();
        cfg.xmlFilePath = pathToNewXmlFile;
        try {
            cfg.save();
        } catch (IOException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            printHelp();
            return;
        }
        int option = Integer.valueOf(args[0]);


        try {
            if (option == Manager.XML31_TO_50) {
                if (args.length < 2) {
                    printHelp();
                    return;
                }
                String pathToXmlFile = args[1];
                update3150(pathToXmlFile);
            } else if (option == Manager.XML40_TO_50) {
                if (args.length != 2) {
                    printHelp();
                    return;
                }
                String pathToXmlFile = args[1];
                update4050(pathToXmlFile);
            } else if (option == Manager.DCUESTA2XML) {
                if (args.length != 8) {
                    printHelp();
                    return;
                }
                String pathToBlocksFile = args[1];
                String pathToCouplingsFile = args[2];
                int cellSizeInMicroMeters = Integer.valueOf(args[3]);
                int maxLengthInCells = Integer.valueOf(args[4]);
                int maxWidthInCells = Integer.valueOf(args[5]);
                int numLayers = Integer.valueOf(args[6]);
                String pathToXmlFile = args[7];
                fromTxtToXml(pathToBlocksFile, pathToCouplingsFile, cellSizeInMicroMeters, maxLengthInCells, maxWidthInCells, numLayers, pathToXmlFile);
            } else if (option == Manager.XML_STATS) {
                if (args.length != 2) {
                    printHelp();
                    return;
                }
                String pathToXmlFile = args[1];
                printStats(pathToXmlFile);
            } else if (option == Manager.XML2OPLDAT1) {
                if (args.length != 4) {
                    printHelp();
                    return;
                }
                String pathToXmlFile = args[1];
                int powerProfile = Integer.valueOf(args[2]);
                String pathToDatFile = args[3];
                fromXmlToDat(pathToXmlFile, powerProfile, pathToDatFile);
            } else if (option == Manager.XML2OPLDAT2) {
                if (args.length != 4) {
                    printHelp();
                    return;
                }
                String pathToXmlFile = args[1];
                int powerProfile = Integer.valueOf(args[2]);
                String pathToDatFile = args[3];
                fromXmlToDatTsvs(pathToXmlFile, powerProfile, pathToDatFile);
            } else if (option == Manager.FIX_LIMITS) {
                if (args.length != 3) {
                    printHelp();
                    return;
                }
                String pathToXmlFile = args[1];
                String pathToNewXmlFile = args[2];
                fixLimits(pathToXmlFile, pathToNewXmlFile);
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void printHelp() {
        System.out.println("Menú:");
        System.out.println("-----");
        System.out.println("1.- Actualizar XML de versión 3.1 a versión 5.0 (1 PathToXml3.1)");
        System.out.println("2.- Actualizar XML de versión 4.0 a versión 5.0 (2 PathToXml4.0)");
        System.out.println("3.- Generar XML de dos archivos de texto (formato David Cuesta, 3 BlocksTxt CouplingsTxt CellSize[e.g.300] LengthInCells WidthInCells NumberOfLayers NewXmlFilePath)");
        System.out.println("4.- Extraer características de XML (4 XmlFilePath)");
        System.out.println("5.- Generar DAT (para OPL, modelo global) de XML " + FloorplanConfiguration.VERSION + " (5 PathToXml IdxPowerProfile NewDatFilePath)");
        System.out.println("6.- Generar DAT (para OPL, modelo TSVs) de XML " + FloorplanConfiguration.VERSION + " (6 PathToXml IdxPowerProfile NewDatFilePath)");
        System.out.println("7.- Fix lower and upper limits (7 PathToXml PathToNewXml)");
    }
}
