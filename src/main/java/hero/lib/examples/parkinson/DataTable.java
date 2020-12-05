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
package hero.lib.examples.parkinson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Class to manage a data table. The data table is passed
 * to this class as a regular data table.
 *
 * @author José Luis Risco Martín
 * @author Josué Pagán Ortiz
 */
public class DataTable {
    
    private static final Logger logger = Logger.getLogger(DataTable.class.getName());
    
    protected ParkinsonClassifier problem;
    protected ArrayList<double[]> table = new ArrayList<>();
    protected int idxBegin = -1;
    protected int idxEnd = -1;
    protected int numInputColumns = 0;
    protected int numTotalColumns = 0;
    protected double[] xLs = null;
    protected double[] xHs = null;
    
    protected String foot = null;
    protected Double patientPDLevel = null;
    protected ArrayList<double[]> clinicalTable = new ArrayList<>();
    protected int lengthIni = 0;
    protected int lengthEnd = 0;
    protected int[][] limitMarkers;
    protected int[][] patientsIdxs;
    protected String exercises;
    protected String[] exercisesTrunc;
    
    protected String rawData;
    protected String clinicData;
    
    public DataTable(ParkinsonClassifier problem, String type, int idxBegin, int idxEnd) throws IOException {
        this.problem = problem;
        logger.info("Reading data file ...");
        setPaths(type);
        readData(clinicData, clinicalTable, false);

        this.exercises = problem.properties.getProperty("Exercises");
        this.exercisesTrunc = exercises.split(",");
        this.limitMarkers = new int[clinicalTable.size()][2*2*exercisesTrunc.length]; // For two feet
     
        fillDataTable(table);
        this.idxBegin = (idxBegin == -1) ? 0 : idxBegin;
        this.idxEnd = (idxEnd == -1) ? table.size() : idxEnd;
        logger.info("Evaluation interval: [" + this.idxBegin + "," + this.idxEnd + ")");
        logger.info("... done.");
    }
    
    public DataTable(ParkinsonClassifier problem, String type) throws IOException {
        this(problem, type, -1, -1);
    }
    
    public final void fillDataTable(ArrayList<double[]> dataTable) throws IOException {
        numInputColumns = 0;
        numTotalColumns = 0;
        
        for (int p = 0; p < clinicalTable.size(); p++) {
            String patientID = String.valueOf((int)clinicalTable.get(p)[Integer.valueOf(problem.properties.getProperty("IDCol"))]);               // Get the code GAxxxxxx
            patientPDLevel = clinicalTable.get(p)[Integer.valueOf(problem.properties.getProperty("PDLevelCol"))];              // Get the level scale H&Y
            logger.finer("PatientID: GA" + patientID + ", PDlevel: " + patientPDLevel);
            
            for (int ex = 0; ex < exercisesTrunc.length; ex++) { // For each exercise
                
                for (int f = 0; f<=1; f++){	// For each foot
                    lengthIni = table.size();
                    foot = (f == 0) ? "RightFoot_" : "LeftFoot_";
                    
                    String absoluteDataPath = rawData + "/GA" + patientID + "/" + foot + exercisesTrunc[ex] + ".csv";
                    readData(absoluteDataPath, table, true);
                    
                    // Store indexes: from-to for each FOOT
                    if (lengthIni < table.size()-1){
                        limitMarkers[p][4*ex+2*f] = lengthIni;
                        limitMarkers[p][4*ex+2*f+1] = table.size()-1;
                    }
                    else {
                        limitMarkers[p][4*ex+2*f] = -1;
                        limitMarkers[p][4*ex+2*f+1] = -1;
                    }
                }               
            }            
        }
    }
       
    public final void readData(String dataPath, ArrayList<double[]> dataTable, Boolean addOutputLine) throws IOException {
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
                        numTotalColumns = numInputColumns + 1;
                    }
                    
                    double[] dataLine = new double[numTotalColumns];
                    for (int j = 0; j < numInputColumns; j++) {
                        dataLine[j] = Double.valueOf(parts[j]);
                    }
                    if (addOutputLine) {
                        dataLine[numTotalColumns-1] = patientPDLevel;
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
        
    public ArrayList<double[]> getDataTable(String  type) {
        switch (type) {
            case "rawData":
                return table;
            case "clinicalData":
                return clinicalTable;
            default:
                return table;
        }
    }
    
    public ArrayList<double[]> getDataTable(String type, int idx1, int idx2) {
        switch (type) {
            case "rawData":
                return new ArrayList(table.subList(idx1, idx2));
            case "clinicalData":
                return new ArrayList(clinicalTable.subList(idx1, idx2));
            default:                
                return new ArrayList(table.subList(idx1, idx2));
        }
    }

    public int[][] getLimitMarkers(){
        return limitMarkers;
    }

    public int[][] getPatientsIdXs(boolean crossVal){
        // Check N fold cross-validation
        if (crossVal) {
            patientsIdxs = randomizeDataSelection(clinicalTable.size(), Integer.valueOf(problem.properties.getProperty("N")), true);
        } else {
            patientsIdxs = randomizeDataSelection(clinicalTable.size(), 1, false);
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
        String dataPath = problem.properties.getProperty("DataPathBase");
        rawData = (dataPath + problem.properties.getProperty("RawDataPath"));
        switch (type){
            case "training":
                clinicData = (dataPath + problem.properties.getProperty("TrainingClinicalPath"));
                break;
            case "test":
                clinicData = (dataPath + problem.properties.getProperty("TestClinicalPath"));
                break;
        }
    }
}