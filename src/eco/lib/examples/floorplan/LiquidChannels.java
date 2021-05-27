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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import eco.core.algorithm.metaheuristic.moga.NSGAII;
import eco.core.operator.crossover.SinglePointCrossover;
import eco.core.operator.mutation.BooleanMutation;
import eco.core.operator.selection.BinaryTournamentNSGAII;
import eco.core.problem.Problem;
import eco.core.problem.Solution;
import eco.core.problem.Solutions;
import eco.core.problem.Variable;
import eco.core.util.logger.HeroLogger;

public class LiquidChannels extends Problem<Variable<Boolean>> {

    private static final Logger logger = Logger.getLogger(LiquidChannels.class.getName());
    /**
     * TODO: (To be improved) Initial temperature of the liquid channels (in K)
     */
    public static final double TEMP_LC = 300.15; // 27 ºC
    /**
     * TODO: Numer of liquid channels in total.
     * The current value is just for testing purposes.
     */
    protected int numLCs; //Number of liquid channels
    protected FloorplanConfiguration configuration;
    protected double[][][] temperatures;
    protected double maxTemp = Double.NEGATIVE_INFINITY;

    public LiquidChannels(FloorplanConfiguration configuration, int numLCs) {
        super(configuration.maxLengthInCells * configuration.numLayers, 2);
        this.configuration = configuration;
        this.numLCs = numLCs;
        temperatures = new double[configuration.maxLengthInCells][configuration.maxWidthInCells][configuration.numLayers];
        // Read temperatures:
        try {
            File file = new File(configuration.xmlFilePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xmlDoc = builder.parse(file.toURI().toString());
            NodeList xmlTemperatureList = xmlDoc.getElementsByTagName("Temperature");
            for (int i = 0; i < xmlTemperatureList.getLength(); ++i) {
                Element xmlTemperature = (Element) xmlTemperatureList.item(i);
                int x = Integer.valueOf(xmlTemperature.getAttribute("x"));
                int y = Integer.valueOf(xmlTemperature.getAttribute("y"));
                int z = Integer.valueOf(xmlTemperature.getAttribute("z"));
                double temperature = Double.valueOf(xmlTemperature.getAttribute("value"));
                if (temperature > maxTemp) {
                    maxTemp = temperature;
                }
                temperatures[x][y][z] = temperature;
            }
        } catch (SAXException ex) {
            Logger.getLogger(LiquidChannels.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LiquidChannels.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(LiquidChannels.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

  @Override
    public Solutions<Variable<Boolean>> newRandomSetOfSolutions(int size) {
      Solutions<Variable<Boolean>> solutions = new Solutions<Variable<Boolean>>();
        ArrayList<Integer> idxs = new ArrayList<Integer>();
        for (int i = 0; i < numberOfVariables; ++i) {
            idxs.add(i);
        }
        for (int i=0; i<size; ++i) {
          Solution<Variable<Boolean>> solution = new Solution<Variable<Boolean>>(numberOfObjectives);
          for (int j = 0; j < numberOfVariables; ++j) {
            solution.getVariables().add(new Variable<Boolean>(false));
          }
            Collections.shuffle(idxs);
            for (int j = 0; j < numberOfVariables; ++j) {
                int idx = idxs.get(j);
                boolean varJ = false;
                if (j < numLCs) {
                    varJ = true;
                }
                solution.getVariables().set(idx, new Variable<Boolean>(varJ));
            }
            solutions.add(solution);
        }
        return solutions;
    }
  
  @Override
  public void evaluate(Solutions<Variable<Boolean>> solutions) {
    for(Solution<Variable<Boolean>> solution : solutions) {
      evaluate(solution);
    }
  }

    public void evaluate(Solution<Variable<Boolean>> solution) {
        int unfeasibilities = 0;

        // Create the new temperatures' scenario
        double[][][] newTemp = new double[configuration.maxLengthInCells][configuration.maxWidthInCells][configuration.numLayers];
        for (int i = 0; i < configuration.maxLengthInCells; ++i) {
            for (int j = 0; j < configuration.maxWidthInCells; ++j) {
                for (int k = 0; k < configuration.numLayers; ++k) {
                    newTemp[i][j][k] = temperatures[i][j][k];
                }
            }
        }
        double[][] tempLC = new double[super.numberOfVariables][configuration.maxWidthInCells];
        for (int i = 0; i < super.numberOfVariables; ++i) {
            for (int j = 0; j < configuration.maxWidthInCells; ++j) {
                tempLC[i][j] = TEMP_LC;
            }
        }

        /**
         * TODO: First approximation, please, improve:
         */
        ArrayList<Variable<Boolean>> variables = solution.getVariables();
        // Los canales deben estar al menos separados tres celdas
        // ESTO HAY QUE QUITARLO EN  UN FUTURO, mejorando la función de bajada de temperatura
        for (int i = 0; i < numberOfVariables - 3; ++i) {
            if (variables.get(i).getValue()) {
                if (variables.get(i + 1).getValue()) {
                    unfeasibilities++;
                }
                if (variables.get(i + 2).getValue()) {
                    unfeasibilities++;
                }
                if (variables.get(i + 3).getValue()) {
                    unfeasibilities++;
                }
            }
        }
        // We count the number of liquid channels:
        int currentLC = 0, z = -1, x = 0;
        for (int i = 0; i < numberOfVariables; ++i) {
            if (i % configuration.maxLengthInCells == 0) {
                z++;
                x = 0;
            } else {
                x++;
            }
            boolean value = variables.get(i).getValue();
            if (value) {
                // We must check if there is a TSVs crossing this X:
                /*for (ThermalVia siliconVia : configuration.thermalVias) {
                if (siliconVia.zEnd <= z && siliconVia.x == x) {
                unfeasibilities++;
                }
                }*/
                for (int y = 0; y < configuration.maxWidthInCells; ++y) {
                    double diff = newTemp[x][y][z] - tempLC[currentLC][y];
                    if (diff > 1) {
                        newTemp[x][y][z] -= Math.log(diff);
                    }
                    if (x > 0) {
                        double diffX = newTemp[x - 1][y][z] - tempLC[currentLC][y];
                        if (diffX > 1) {
                            newTemp[x - 1][y][z] -= 0.75 * Math.log(diffX);
                        }
                    }
                    if (x < configuration.maxLengthInCells - 1) {
                        double diffX = newTemp[x + 1][y][z] - tempLC[currentLC][y];
                        if (diffX > 1) {
                            newTemp[x + 1][y][z] -= 0.75 * Math.log(diffX);
                        }
                    }
                    // Tenemos que hacer lo mismo que el simulador. El simulador no calienta el agua:
                    //if (y + 1 < configuration.maxWidthInCells) {
                    //  tempLC[currentLC][y + 1] += Math.log10(diff);
                    //}
                }
                currentLC++;
            }
        }
        unfeasibilities += Math.abs(numLCs - currentLC);


        double fitness = 0.0;
        // Compute average temperature
        /*
        for (int i = 0; i < configuration.maxLengthInCells; ++i) {
        for (int j = 0; j < configuration.maxWidthInCells; ++j) {
        for (int k = 0; k < configuration.numLayers; ++k) {
        fitness += newTemp[i][j][k];
        }
        }
        }
        fitness = fitness / (configuration.maxLengthInCells * configuration.maxWidthInCells * configuration.numLayers);
         */

        // Compute sum of temperatures
        for (int i = 0; i < configuration.maxLengthInCells; ++i) {
            for (int j = 0; j < configuration.maxWidthInCells; ++j) {
                for (int k = 0; k < configuration.numLayers; ++k) {
                    fitness += newTemp[i][j][k];
                }
            }
        }

        solution.getObjectives().set(0, 1.0 * unfeasibilities);
        if (unfeasibilities > 0) {
            solution.getObjectives().set(1, maxTemp + unfeasibilities);
        } else {
            solution.getObjectives().set(1, fitness);
        }

    }

    public void saveFloorplanFile(FloorplanConfiguration configuration, ArrayList<Variable<Boolean>> variables, Solution<Variable<Boolean>> solution, String rutaXml) throws ParserConfigurationException, IOException, SAXException {
    /*    int MyCellSize = configuration.cellSizeInMicroMeters;
        int MiL = configuration.maxLengthInCells; //Eje X
        int MiW = configuration.maxWidthInCells; //Eje Y
        int MisCapas = configuration.numLayers;
        int num_channels = 0;
        int[] pos_x = new int[numLCs];
        int[] capa = new int[numLCs];

        for (int h = 1; h < (MisCapas * 4) + 1; h++) {
            for (int i = 0; i < MiW; i++) {
                for (int j = 0; j < MiL + 1; j++) { //This is because the first and the last row are for dioxide
                    if (h % 4 == 1) { //It is SiO2 represented by the letter 'd'
                        if (j == MiL) {
                            //  writer.write("d\n");
                        } else {
                            //    writer.write('d');
                        }
                    }
                    if (h % 4 == 2) { //It is always silicon, it is represented by 's'
                        if (j == MiL) {
                            //      writer.write("s\n");
                        } else {
                            //        writer.write('s');
                        }
                    }
                    if (h % 4 == 3) { //It is SiO2 with water channels if there are any
                        String Canal = "d";
                        if (j == 0) {
                            //          writer.write("d");
                        } else {
                            boolean Canales = variables.get(h / 4 * MiL + j - 1).getValue();
                            if (j == MiL) {
                                //                writer.write("d\n");
                            } else {
                                if (Canales == true) { //There is a channel
                                    //writer.write('w');
                                    if (i == 1) {
                                        pos_x[num_channels] = j;
                                        capa[num_channels] = h;
                                        num_channels = num_channels + 1;
                                    }
                                } else {
                                    //   writer.write('d');
                                }

                            }
                        }
                    }
                    if (h % 4 == 0) { //It is always epoxy (glue layer) or SiO2 if it is the last layer
                        if (h == (MisCapas * 4)) {
                            if (j == MiL) {
                                //          writer.write("d\n");
                            } else {
                                //        writer.write('d');
                            }
                        } else {
                            if (j == MiL) {
                                //          writer.write("b\n");
                            } else {
                                //        writer.write('b');
                            }
                        }
                    }
                }
            }
        }

        //System.out.println("Los canales están en las x: "+ pos_x.toString());
        //System.out.println("Los canales están en las capas: "+ capa.toString());
        try {

            // Implementación DOM por defecto de Java
            // Construimos nuestro DocumentBuilder
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            // Procesamos el fichero XML y obtenemos nuestro objeto Document
            Document doc = documentBuilder.parse(new InputSource(new FileInputStream(rutaXml)));

            // Obtenemos la etiqueta raiz
            Element elementRaiz = doc.getDocumentElement();
            Element channels = doc.createElement("LiquidChannels");
            elementRaiz.appendChild(channels);


            // Buscamos los Block dentro del XML

            NodeList listaNodos = doc.getElementsByTagName("Block");
            int num_blocks = listaNodos.getLength();
            for (int i = 0; i < num_blocks; i++) {
                Node nodo = listaNodos.item(i);
                //if (nodo instanceof Element) {
                //    System.out.println(nodo.getTextContent());
                //}
            }

            for (int i = 0; i < num_channels; i++) {
                Element channelblock = doc.createElement("LiquidChannel");
                channels.appendChild(channelblock);
                channelblock.setAttribute("id", (num_blocks + i + 1) + "");
                channelblock.setAttribute("x", pos_x[i] + "");
                channelblock.setAttribute("z", ((capa[i] + 1) / 4) + "");
            }
            // Vamos a convertir el arbol DOM en un String
            // Definimos el formato de salida: encoding, identación, separador de línea,...
            // Pasamos doc como argumento para tener un formato de partida
            OutputFormat format = new OutputFormat(doc);
            format.setLineSeparator(LineSeparator.Unix);
            format.setIndenting(true);
            format.setLineWidth(0);
            format.setPreserveSpace(false);
            // Definimos donde vamos a escribir. Puede ser cualquier OutputStream o un Writer
            CharArrayWriter salidaXML = new CharArrayWriter();
            // Serializamos el arbol DOM
            XMLSerializer serializer = new XMLSerializer((Writer) salidaXML, format);
            serializer.asDOMSerializer();
            serializer.serialize(doc);
            // Ya tenemos el XML serializado en el objeto salidaXML

//Ahora lo pasamos a un fichero dentro de test
            FileWriter fichero = null;

            try {
                String[] nombre = rutaXml.split("_");
                String mi_ruta = nombre[0] + "_3.xml";
                fichero = new FileWriter(mi_ruta);
                PrintWriter pw = null;

                pw = new PrintWriter(fichero);

                pw.println(salidaXML.toString());

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    // Nuevamente aprovechamos el finally para
                    // asegurarnos que se cierra el fichero.
                    if (null != fichero) {
                        fichero.close();
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }*/
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java -jar LiquidChannels.jar <PathToXml> <MaxNumOfLiquidChannels>");
            return;
        }
        HeroLogger.setup(Level.INFO);
        String rutaXml = args[0];
        Integer numLCs = Integer.valueOf(args[1]);

        FloorplanConfiguration configuration = new FloorplanConfiguration(rutaXml);
        LiquidChannels liquidC = new LiquidChannels(configuration, numLCs);
        // Parece que el resultado es independiente de los operadores utilizados, seguramente
        // se deba a que el modelo de temperaturas es un poco escueto:
        //NSGAII nsga2 = new NSGAII(liquidC, 100, 250, new SwapMutation(1.0 / liquidC.getNumberOfVariables()), new LiquidChannelsCrossover(0.9, NUM_LCS), new BinaryTournamentNSGAII());
        NSGAII<Variable<Boolean>> nsga2 = new NSGAII<Variable<Boolean>>(liquidC, 100, 2500, new BooleanMutation<Variable<Boolean>>(1.0 / liquidC.getNumberOfVariables()), new SinglePointCrossover<Variable<Boolean>>(liquidC), new BinaryTournamentNSGAII<Variable<Boolean>>());
        nsga2.initialize();
        Solutions<Variable<Boolean>> solutions = nsga2.execute();
        Logger.getLogger(LiquidChannels.class.getName()).info("solutions.size()=" + solutions.size());

        Solution<Variable<Boolean>> bestSolution = null;
        double bestObjective = Double.POSITIVE_INFINITY;
        for (Solution<Variable<Boolean>> solution : solutions) {
            if (solution.getObjectives().get(0) <= 0 && solution.getObjectives().get(1) < bestObjective) {
                bestObjective = solution.getObjectives().get(1);
                bestSolution = solution;
            }
        }
        Logger.getLogger(LiquidChannels.class.getName()).info("Best solution: " + bestSolution.toString());

        //liquidC.printStatistics();
        try {
            liquidC.saveFloorplanFile(configuration, bestSolution.getVariables(), bestSolution, rutaXml);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void printStatistics() {
        double[][] avgTempByXZ = new double[configuration.maxLengthInCells][configuration.maxWidthInCells];
        for (int x = 0; x < configuration.maxLengthInCells; ++x) {
            for (int z = 0; z < configuration.numLayers; ++z) {
                avgTempByXZ[x][z] = 0.0;
            }
        }

        for (int z = 0; z < configuration.numLayers; ++z) {
            for (int x = 0; x < configuration.maxLengthInCells; ++x) {
                for (int y = 0; y < configuration.maxWidthInCells; ++y) {
                    avgTempByXZ[x][z] += temperatures[x][y][z];
                }
            }
        }

        for (int z = 0; z < configuration.numLayers; ++z) {
            for (int x = 0; x < configuration.maxLengthInCells; ++x) {
                avgTempByXZ[x][z] /= configuration.maxWidthInCells;
            }
        }

        for (int z = 0; z < configuration.numLayers; ++z) {
            for (int x = 0; x < configuration.maxLengthInCells; ++x) {
                Logger.getLogger(LiquidChannels.class.getName()).info("(z,x,t) = (" + z + "," + x + "," + avgTempByXZ[x][z] + ")");
            }
        }
    }

    @Override
    public Problem<Variable<Boolean>> clone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
