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
 *  - Josué Pagán Ortíz
 *  - José Luis Risco Martín
 */
package eco.lib.examples.parkinson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

import eco.core.util.StringManagement;

/**
 * Class to manage a data table. The data table is passed
 * to this class as a regular data table.
 *
 * @author José Luis Risco Martín
 * @author Josué Pagán Ortiz
 */
public class FeaturesTable {
    
    private static final Logger logger = Logger.getLogger(FeaturesTable.class.getName());
    
    protected Properties problemProperties;
    protected ArrayList<double[]> table = new ArrayList<>();
    protected ArrayList tableNames = new ArrayList<>();

    protected int idxBegin = -1;
    protected int idxEnd = -1;
    protected int numInputColumns = 0;
    protected double[] xLs = null;
    protected double[] xHs = null;
    
    protected int lengthIni = 0;
    protected int lengthEnd = 0;
    protected int[][] patientsIdxs;
    
    protected String features;
    protected String featuresNames;


    
    public FeaturesTable(Properties problemProperties, String type, int idxBegin, int idxEnd) throws IOException {
        this.problemProperties = problemProperties;
        logger.info("Reading data file ...");
        setPaths(type);
        readData(features, table);
        numInputColumns = 0;
        readHead(featuresNames, tableNames);

        this.idxBegin = (idxBegin == -1) ? 0 : idxBegin;
        this.idxEnd = (idxEnd == -1) ? table.size() : idxEnd;
        logger.info("Evaluation interval: [" + this.idxBegin + "," + this.idxEnd + ")");
        logger.info("... done.");
    }
    
    public FeaturesTable(Properties problemProperties, String type) throws IOException {
        this(problemProperties, type, -1, -1);
    }
    
       
    public final void readData(String dataPath, ArrayList<double[]> dataTable) throws IOException {
        File file = new File(dataPath);
        if (file.exists()){
            
            try (BufferedReader reader = new BufferedReader(new FileReader(new File(dataPath)))) {
                String line;
                
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split(";");
                    if (parts.length == 1){
                        parts = line.split(",");
                    }
                    if (parts.length > numInputColumns) {
                        numInputColumns = parts.length;
                    }
                    
                    double[] dataLine = new double[numInputColumns];
                    for (int j = 0; j < numInputColumns; j++) {
                        dataLine[j] = Double.valueOf(parts[j]);
                    }
                    dataTable.add(dataLine);
                }
                reader.close();
            }
        }
        else {
            logger.finer("File: " + dataPath + " DOES NOT EXIST");
        }
    }
        

    public final void readHead(String dataPath, ArrayList headTable) throws IOException {
        File file = new File(dataPath);
        if (file.exists()){
            
            try (BufferedReader reader = new BufferedReader(new FileReader(new File(dataPath)))) {
                String line;
                
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split(";");
                    if (parts.length == 1){
                        parts = line.split(",");
                    }
                    if (parts.length > numInputColumns) {
                        numInputColumns = parts.length;
                    }
                    
                    for (int j = 0; j < numInputColumns; j++) {
                        headTable.add(parts[j]);
                    }
                    
                }
                reader.close();
            }
        }
        else {
            logger.finer("File: " + dataPath + " DOES NOT EXIST");
        }
    }

    public ArrayList<double[]> getFeaturesTable(String type) {
         switch (type) {
            case "features":
                return table;
            case "names":
                return tableNames;
            default:
                return table;
        }
    }
    
    public ArrayList<double[]> getFeaturesTable(int idx1, int idx2) {
        return new ArrayList(table.subList(idx1, idx2));           
    }

    public int[][] getPatientsIdXs(boolean crossVal) throws IOException{
        // Check N fold cross-validation
        if (crossVal) {
            patientsIdxs = randomizeDataSelection(table.size(), Integer.valueOf(problemProperties.getProperty("N")), true);
        } else {
            patientsIdxs = randomizeDataSelection(table.size(), 1, false);
        }
        return patientsIdxs;
    }
    
    public int[][] getPatientsIdXs(String fileIdxsPatients) throws IOException{
            ArrayList<double[]> tempIdxs = new ArrayList<>();
            readData(problemProperties.getProperty("DataPathBase") + fileIdxsPatients, tempIdxs);
            patientsIdxs = new int[1][tempIdxs.size()];
            
            for (int i=0; i<tempIdxs.size(); i++){
                patientsIdxs[0][i] = (int)tempIdxs.get(i)[0];
            }
        
        return patientsIdxs;
    }
    
    public int[][] randomizeDataSelection(int elements, int groups, boolean randomize){
        int[][] randomTable = new int[groups][elements/groups];
        int[] elementsAvailable = new int[elements];
        
        for (int i=0; i<= elements-1; i++){
            elementsAvailable[i] = i;
        }
        if (randomize) {
            // Implementing Fisher–Yates shuffle
            Random rnd = new Random();
            for (int i = elementsAvailable.length - 1; i > 0; i--) {
                int index = rnd.nextInt(i + 1);
                // Simple swap
                int a = elementsAvailable[index];
                elementsAvailable[index] = elementsAvailable[i];
                elementsAvailable[i] = a;
            }
        }
        
        for (int i=0; i<= groups-1; i++){
            for (int j=0; j<= (elements/groups)-1; j++){
                randomTable[i][j] = elementsAvailable[j+(i*elements/groups)];
            }
        }
        return randomTable;
    }
    
    public final void setPaths(String type) {
        String dataPath = problemProperties.getProperty("DataPathBase");
        featuresNames = (dataPath + problemProperties.getProperty("FeaturesNamesPath"));

        switch (type) {
            case "training":
                features = (dataPath + problemProperties.getProperty("FeaturesTrainingPath"));
                break;
            case "test":
                features = (dataPath + problemProperties.getProperty("FeaturesTestPath"));
                break;
        }
    }
}