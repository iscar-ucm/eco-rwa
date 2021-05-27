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
package eco.unstable.util.classifier;

/**
 * Class to manage the confusion matrix and metric parameters of a classifier.
 *
 * @author José Luis Risco Martín
 * @author Josué Pagán Ortiz
 */
public class ClassifierEvaluator {
    protected int[][] confusionMatrix;
    protected double[] original;
    protected double[] predicted;
    
    public ClassifierEvaluator(int c) {
        this.confusionMatrix = new int[c][c];
    }
    
    public void resetConfusionMatrix() {
        for (int i=0; i<confusionMatrix.length; i++) {
            for (int j=0; j<confusionMatrix.length; j++) {
                confusionMatrix[i][j] = 0;
            }
        }
    }
    
    public void setConfusionMatrix(int[][] cm) {
        confusionMatrix = cm;
    }
    
    public void setValue(int element, int originalClass, int classifiedClass, int v) {
        confusionMatrix[classifiedClass][originalClass] += v;
        if (element > 0){
            original[element] = originalClass;
            predicted[element] = classifiedClass;            
        }
    }
    
    public void setValue(int originalClass, int classifiedClass, int v) {
        setValue(-1, originalClass, classifiedClass, v);
    }
    
    public int[][] getConfusionMatrix() {
        return confusionMatrix;
    }
    
    public int getN() {
        int n = 0;
        for (int i=0; i < confusionMatrix.length; i++) {
            for (int j=0; j < confusionMatrix.length; j++) {
                n += confusionMatrix[i][j];
            }
        }
        return n;
    }
    
    public double[] getOriginal(){
        return original;
    }

    public double[] getPredicted(){
        return predicted;
    }

    // Calculate the OSR (Overall Success Rate)
    public double getClassificationRate() {
        double osr = 0.0;
        for (int i=0; i < confusionMatrix.length; i++) {
            osr += confusionMatrix[i][i];
        }
        return (double)osr/getN();
    }
    
    // Marginal rates functions
    public double getSensitivity(int classC) { // Or TPR
        if (getTruePositives(classC) + getFalseNegatives(classC) == 0) {
            return 0.0;
        }
        return (double)getTruePositives(classC)/(getTruePositives(classC)+getFalseNegatives(classC));
    }
    
    public double getSpecificity(int classC) {  // Or TNR
        if (getTrueNegatives(classC)+getFalsePositives(classC) == 0) {
            return 0.0;
        }
        return (double)getTrueNegatives(classC)/(getTrueNegatives(classC)+getFalsePositives(classC));
    }
    
    public double getPrecision(int classC) { // Or PPV
        if (getTruePositives(classC)+getFalsePositives(classC) == 0) {
            return 0.0;
        }
        return (double)getTruePositives(classC)/(getTruePositives(classC)+getFalsePositives(classC));
    }
    
    public double getFValue(int classC){
        return 2/((1/(getSensitivity(classC)))+(1/(getPrecision(classC))));
    }
    
    // Micro-averaged values
    public double getMicroAveragePrecision(){
        int sumAllTP = 0;
        int sumAllFP = 0;
        
        for (int i=0; i<confusionMatrix.length; i++) {
            sumAllTP += getTruePositives(i);
            sumAllFP += getFalsePositives(i);
        }
        return (double)sumAllTP/(sumAllTP+sumAllFP);
    }
    
    public double getMicroAverageSensitivity(){
        int sumAllTP = 0;
        int sumAllFN = 0;
        
        for (int i=0; i<confusionMatrix.length; i++) {
            sumAllTP += getTruePositives(i);
            sumAllFN += getFalseNegatives(i);
        }
        return (double)sumAllTP/(sumAllTP+sumAllFN);
    }
    
    public double getMicroFValue(){
        return 2/((1/(getMicroAveragePrecision()))+(1/(getMicroAverageSensitivity())));
    }
    
    // Macro-averaged values
    public double getMacroAveragePrecision(){
        double sumAllPrecisions = 0.0;
        
        for (int i=0; i<confusionMatrix.length; i++) {
            sumAllPrecisions += getPrecision(i);
        }
        return sumAllPrecisions/confusionMatrix.length;
    }
    
    public double getMacroAverageSensitivity(){
        double sumAllSensitivities = 0.0;
        
        for (int i=0; i<confusionMatrix.length; i++) {
            sumAllSensitivities += getSensitivity(i);
        }
        return sumAllSensitivities/confusionMatrix.length;
    }
    
     public double getMacroAverageSpecificity(){
        double sumAllSpecificities = 0.0;
        
        for (int i=0; i<confusionMatrix.length; i++) {
            sumAllSpecificities += getSpecificity(i);
        }
        return sumAllSpecificities/confusionMatrix.length;
    }
     
    public double getMacroFValue(){
        return 2/((1/(getMacroAveragePrecision()))+(1/(getMacroAverageSensitivity())));
    }
    
      
    // Marginal rates
    // Marginal true positives
    public int getTruePositives(int classC) {
        return confusionMatrix[classC][classC];
    }
    
    // Marginal false positives
    public int getFalsePositives(int classC) {
        int niC = 0;
        for (int i=0; i < confusionMatrix.length; i++) {
            niC += confusionMatrix[classC][i];
        }
        return niC-confusionMatrix[classC][classC];
    }
    
    // Marginal false negatives
    public int getFalseNegatives(int classC) {
        int nCi = 0;
        for (int i=0; i < confusionMatrix.length; i++) {
            nCi += confusionMatrix[i][classC];
        }
        return nCi-confusionMatrix[classC][classC];
    }
    
    // Marginal true negatives
    public int getTrueNegatives(int classC) {
        return getN()-getTruePositives(classC)-getFalsePositives(classC)-getFalseNegatives(classC);
    }
}