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
package hero.lib.examples.floorplan.register;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
public class FloorplanRegisterConfiguration {

    public static final String VERSION = "5.0";
    protected String xmlFilePath;
    protected String xmlVersion = FloorplanRegisterConfiguration.VERSION;
    protected int cellSizeInMicroMeters, maxLengthInCells, maxWidthInCells, numPowerProfiles;
    protected double maxDP = Double.MIN_VALUE;
    protected ArrayList<Register> components;

    // Attributes that are only available if the floorplan has been
    // optimized:
    //
    protected FloorplanRegisterConfiguration(String xmlFilePath) {
        this.xmlFilePath = xmlFilePath;
        components = new ArrayList<Register>();
        try {
            load();
            for (Register c : components) {
                for (int i = 0; i < c.dps.length; ++i) {
                    if (c.dps[i] > maxDP) {
                        maxDP = c.dps[i];
                    }
                }
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(FloorplanRegisterConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FloorplanRegisterConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(FloorplanRegisterConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public FloorplanRegisterConfiguration(String xmlFilePath, int cellSizeInMicroMeters, int maxLengthInCells, int maxWidthInCells, int numPowerProfiles, ArrayList<Register> components) {
        this.xmlFilePath = xmlFilePath;
        this.cellSizeInMicroMeters = cellSizeInMicroMeters;
        this.maxLengthInCells = maxLengthInCells;
        this.maxWidthInCells = maxWidthInCells;
        this.numPowerProfiles = numPowerProfiles;
        this.components = components;
        for (Register c : components) {
            for (int i = 0; i < c.dps.length; ++i) {
                if (c.dps[i] > maxDP) {
                    maxDP = c.dps[i];
                }
            }
        }
    }

    @Override
    public FloorplanRegisterConfiguration clone() {
        FloorplanRegisterConfiguration clone = new FloorplanRegisterConfiguration(this.xmlFilePath);
        return clone;
    }

    private void load() throws ParserConfigurationException, IOException, SAXException {
        Logger.getLogger(FloorplanRegisterConfiguration.class.getName()).fine("Loading " + xmlFilePath + "...");
        components.clear();
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
            int l = Double.valueOf(xmlBlock.getAttribute("l")).intValue();
            int w = Double.valueOf(xmlBlock.getAttribute("w")).intValue();
            double[] dps = new double[numPowerProfiles];
            for (int j = 0; j < numPowerProfiles; ++j) {
                dps[j] = Double.valueOf(xmlBlock.getAttribute("dp" + j)).doubleValue();
            }
            Register component = new Register(id, name, x, xMin, xMax, y, yMin, yMax, l, w, dps);
            components.add(component);
        }

        Logger.getLogger(FloorplanRegisterConfiguration.class.getName()).fine("done.");
    }

    public void save() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(xmlFilePath)));
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");

        writer.write("<Floorplan Version=\"" + FloorplanRegisterConfiguration.VERSION + "\" CellSize=\"" + cellSizeInMicroMeters + "\" Length=\"" + maxLengthInCells + "\" Width=\"" + maxWidthInCells + "\" NumLayers=\"1\" NumPowerProfiles=\"" + numPowerProfiles + "\">\n");

        writer.write("\t<Blocks>\n");
        for (Register component : components) {
            writer.write("\t\t<Block id=\"" + component.id + "\" name=\"" + component.name + "\" type=\"2\" xMin=\"" + component.xMin + "\" x=\"" + component.x + "\" xMax=\"" + component.xMax + "\" yMin=\"" + component.yMin + "\" y=\"" + component.y + "\" yMax=\"" + component.yMax + "\" zMin=\"0\" z=\"0\" zMax=\"0\" l=\"" + component.l + "\" w=\"" + component.w + "\" h=\"1\"");
            for (int i = 0; i < numPowerProfiles; ++i) {
                writer.write(" dp" + i + "=\"" + component.dps[i] + "\"");
            }
            writer.write("/>\n");
        }

        writer.write("\t</Blocks>\n");
        writer.write("\t<Couplings>\n");
        writer.write("\t</Couplings>\n");
        writer.write("\t<ThermalVias>\n");
        writer.write("\t</ThermalVias>\n");
        writer.write("</Floorplan>\n");
        writer.flush();
        writer.close();
    }

}
