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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class implementing the simulated annealing technique for problem
 * solving.
 *
 * @author J. M. Colmenar
 */
public class FloorplanDTSSimulatedAnnealing {

    private static final Logger LOGGER = Logger.getLogger(FloorplanDTSSimulatedAnnealing.class.getName());

    private static Properties PROPS = new Properties();

    
    /**
     * Loads properties from file
     */
    private static void loadProperties(String fileName) {
        FileInputStream in;
        try {
            in = new FileInputStream(fileName);
            PROPS.load(in);
            in.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Properties file error: " + e.getMessage());
            System.exit(-1);
        }
        System.out.println("\n#--------- Properties for this run ---------\n");
        for (String str : PROPS.stringPropertyNames()) {
            System.out.println("  " + str + "=" + PROPS.getProperty(str));
        }
        System.out.println("\n#-------------------------------------------\n");
    }    

    
    public static void main(String[] args) {
        
        if (args.length != 2) {
            System.out.println("Parameters: ConfigurationFile OutputDir");
            args = new String[2];
            args[0] = "test_fp" + File.separator + "cfg.properties";
            args[1] = "test_fp" + File.separator + "sols";
//            return;
        }        
        
        loadProperties(args[0]);
        
        // Directory to save XML files and results (could be different than XML file path)
        String outputDir = args[1];

        String xmlFilePath = PROPS.getProperty("XmlFile");
        String[] tokens = xmlFilePath.split(File.separator);
        String xmlFileName = tokens[tokens.length-1];

        Long iter = Long.valueOf(PROPS.getProperty("SimAnnIterations"));
        Double kValue = Double.valueOf(PROPS.getProperty("K_value"));
        Double wiringWeight = Double.valueOf(PROPS.getProperty("WiringFitnessWeight"));
        Double temperatureWeight = Double.valueOf(PROPS.getProperty("TemperatureFitnessWeight"));
        Boolean stopWhenFeasible;
        if (PROPS.getProperty("StopWhenFeasible").equals("1")) stopWhenFeasible = true;
        else stopWhenFeasible = false;
        Boolean randomEncoding;
        if (PROPS.getProperty("RandomEncoding").equals("1")) randomEncoding = true;
        else randomEncoding = false;     
        Long seed = Long.valueOf(PROPS.getProperty("RandomSeed"));
        // If 0 is the seed, then random seed is considered
        if (seed == 0)
            seed = (new Random()).nextLong();

        Long maxSeconds = Long.valueOf(0);
        if (PROPS.getProperty("MaxSeconds") != null) maxSeconds = Long.valueOf(PROPS.getProperty("MaxSeconds"));

        FloorplanConfiguration cfg = new FloorplanConfiguration(xmlFilePath);


        // Create floorplan using DTS notation
        FloorplanDTS cfgDTS = new FloorplanDTS(cfg,wiringWeight,temperatureWeight,seed,randomEncoding,outputDir);

        LOGGER.log(Level.INFO,"\n# Initial Tree:\n{0}\n",cfgDTS.toString());

        // Run SA optimization
        LOGGER.log(Level.INFO,"\n# Running SA optimization...\n");
        SimulatedAnnealingSolver solver = new SimulatedAnnealingSolver(iter,kValue,stopWhenFeasible,seed,maxSeconds);
        SimulatedAnnealingSolver.logFile = outputDir + File.separator + xmlFileName + "_log" + ".txt";

        FloorplanDTS optimizedCfgDTS = (FloorplanDTS) solver.solve(cfgDTS);

        LOGGER.log(Level.INFO,"\n# Final Tree:\n{0}\n",optimizedCfgDTS.toString());
        try {
            optimizedCfgDTS.save(outputDir,xmlFileName.replaceAll(".xml", "_FINAL_"+seed+".xml"));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

/*
        // Screen display (just for debug
        Board board = new Board(0, 10);
        board.buildBoard(optimizedCfgDTS.cfg.xmlFilePath);
*/

 }

}
