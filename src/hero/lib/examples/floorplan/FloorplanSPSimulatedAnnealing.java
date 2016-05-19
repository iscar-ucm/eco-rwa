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

import hero.lib.examples.floorplan.util.SimulatedAnnealingSolver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class implementing the simulated annealing technique for problem
 * solving.
 *
 * @author cia
 */
public class FloorplanSPSimulatedAnnealing {

    private static final Logger logger = Logger.getLogger(FloorplanSPSimulatedAnnealing.class.getName());
 
    private static Properties props = new Properties();

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Parameters: ConfigurationFile OutputDir");
            args = new String[2];
            args[0] = "test_fp" + File.separator + "sp" + File.separator + "cfg_sp.properties";
            args[1] = "test_fp" + File.separator + "sp" + File.separator + "sols";
            return;
        }           
        
        // Directory to save XML files and results (could be different than XML file path)
        String outputDir = args[1];
        
        FileInputStream in;
        try {
            in = new FileInputStream(args[0]);
            props.load(in);
            in.close();
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "ConfigurationFile does not exist !!", ex);
            System.out.println("Current dir: " + System.getProperty("user.dir"));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        System.out.println("\n#--------- Properties for this run ---------\n");
        for (String str : props.stringPropertyNames()) {
            System.out.println("  " + str + "=" + props.getProperty(str));
        }
        System.out.println("\n#-------------------------------------------\n");
        
        String xmlFilePath = props.getProperty("XmlFile");
        Long iter = Long.valueOf(props.getProperty("SimAnnIterations"));
        Double kValue = Double.valueOf(props.getProperty("K_value"));
        Double wiringWeight = Double.valueOf(props.getProperty("WiringFitnessWeight"));
        Double temperatureWeight = Double.valueOf(props.getProperty("TemperatureFitnessWeight"));
        Boolean stopWhenFeasible;
        if (props.getProperty("StopWhenFeasible").equals("1")) {
            stopWhenFeasible = true;
        } else {
            stopWhenFeasible = false;
        }
        Long seed = Long.valueOf(props.getProperty("RandomSeed"));
        // If 0 is the seed, then random seed is considered
        if (seed == 0) {
            seed = (new Random()).nextLong();
        }

        Long maxSeconds = Long.valueOf(0);
        if (props.getProperty("MaxSeconds") != null) maxSeconds = Long.valueOf(props.getProperty("MaxSeconds"));

        FloorplanConfiguration cfg = new FloorplanConfiguration(xmlFilePath);

        // Create floorplan using SP notation
        //--TXT Codification file      
        String codifSPfile = (props.getProperty("CodificationFile"));
        ArrayList<ArrayList<Integer>> listSPcodif = new ArrayList<ArrayList<Integer>>();
        listSPcodif = loadDataFile(codifSPfile);
        FloorplanSP fpSP = new FloorplanSP(cfg, listSPcodif, wiringWeight, temperatureWeight, seed);
        // Run SA optimization
        logger.log(Level.INFO, "\n# Running SA optimization...\n");
        SimulatedAnnealingSolver solver = new SimulatedAnnealingSolver(iter, kValue, stopWhenFeasible, seed, maxSeconds);
        String[] tempCad = xmlFilePath.split(File.separator);
        SimulatedAnnealingSolver.logFile = outputDir + File.separator + tempCad[tempCad.length-1] + "_log" + ".txt";

        FloorplanSP optimizedCfgSP = (FloorplanSP) solver.solve(fpSP);
        
        String xmlNew = outputDir + File.separator + tempCad[tempCad.length-1].replaceAll(".xml", "_FINAL_" + seed + ".xml");
        
        try {
            optimizedCfgSP.save(xmlNew);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        /*
        // Screen display (just for debug)
        optimizedCfgSP.isFeasible();
        Board board = new Board(0, 10);
        board.buildBoard(xmlNew);
        */
    }
    
    private static ArrayList<ArrayList<Integer>> loadDataFile(String fileName) {
        /* This method reads 'filename' containing the (gamma-,gamma+) 
         *  SP codification for each layer
         * Returns 'listSPcodif' which is an ArrayList of [gamma-,gamma+]
         *  i.e. each layer is coded by an ArrayList that contains 
         *  gamma- followed by gamma+ 
         * Each one of them are Arraylist of Integer
         */
        ArrayList<ArrayList<Integer>> listSPcodif = new ArrayList<ArrayList<Integer>>();

        FileReader f;
        BufferedReader reader = null;
        String line;
        ArrayList<String> data = new ArrayList<String>();

        try {
            f = new FileReader(fileName);
            reader = new BufferedReader(f);
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && !line.equals("")) {
                    ArrayList<Integer> nums = new ArrayList<Integer>();
                    String[] palabras;
                    //gamma-
                    data.add(line.trim());
                    palabras = line.split(",");
                    for (String pal : palabras) {
                        nums.add(Integer.valueOf(pal));
                    }
                    //gamma+
                    line = reader.readLine();
                    data.add(line.trim());
                    palabras = line.split(",");
                    for (String pal : palabras) {
                        nums.add(Integer.valueOf(pal));
                    }
                    listSPcodif.add(nums);
                }
                // Lines may be comments
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Error --> Data file read failed: " + e);
        }
        return listSPcodif;
    }
}
