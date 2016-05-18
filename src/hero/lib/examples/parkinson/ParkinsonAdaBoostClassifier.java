/*
* Copyright (C) 2010-2015 José Luis Risco Martín <jlrisco@ucm.es>
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import hero.algorithm.metaheuristic.ga.SimpleGeneticAlgorithm;
import hero.algorithm.metaheuristic.moge.AbstractProblemGE;
import hero.algorithm.metaheuristic.moge.Phenotype;
import hero.operator.comparator.SimpleDominance;
import hero.operator.crossover.SinglePointCrossover;
import hero.operator.evaluator.AbstractPopEvaluator;
import hero.operator.mutation.IntegerFlipMutation;
import hero.operator.selection.BinaryTournament;
import hero.optimization.threads.MasterWorkerThreads;
import hero.problem.Solution;
import hero.problem.Solutions;
import hero.problem.Variable;
import hero.unstable.util.classifier.ClassifierEvaluator;
import hero.unstable.util.classifier.Quantizer;
import hero.util.compiler.MyCompiler;
import hero.util.compiler.MyLoader;
import hero.util.logger.HeroLogger;
import hero.unstable.util.Maths;
import hero.unstable.util.classifier.AdaBoost;


public class ParkinsonAdaBoostClassifier extends AbstractProblemGE {
    
    private static final Logger logger = Logger.getLogger(ParkinsonAdaBoostClassifier.class.getName());
    private static boolean whoWas = false;
    
    private static int CURRENT_THREAD_ID = 1;
    private static int CURRENT_FOLD = 0;

    protected int threadId;
    protected MyCompiler compiler;
    protected FeaturesTable featuresTable = null;
    protected Properties properties;
    protected AbstractPopEvaluator evaluator;
    protected ClassifierEvaluator classifierEval;
    protected Quantizer classifier;
    protected String kindClassifier;
    protected int[][] currentData;
    protected int pdLevelCol;
    protected int IDCol;
    
    protected double[][] resultsMatrix;
    protected double[][] bestResultsMatrix;
    
    protected double bestClassRate = Double.NEGATIVE_INFINITY;
    protected double bestMacroAvgTPR = Double.NEGATIVE_INFINITY;
    protected double bestMacroAvgTNR = Double.NEGATIVE_INFINITY;
    protected double bestMacroAvgF = Double.NEGATIVE_INFINITY;
    protected double bestMacroAvgPPV = Double.NEGATIVE_INFINITY;
    protected String bestExpression = null;
    protected Solution<Variable<Integer>> bestSolution = null;
    protected int bestSolIdx;
    protected int bestNumGeneration;
    
    @Override
    public ParkinsonAdaBoostClassifier clone() {
        ParkinsonAdaBoostClassifier clone = null;
        try {
            clone = new ParkinsonAdaBoostClassifier(properties);
            clone.featuresTable = this.featuresTable;
            clone.currentData = this.currentData;
            clone.pdLevelCol = this.pdLevelCol;
            clone.IDCol = this.IDCol;
            
            clone.bestClassRate = Double.NEGATIVE_INFINITY;
            clone.bestMacroAvgTPR = Double.NEGATIVE_INFINITY;
            clone.bestMacroAvgTNR = Double.NEGATIVE_INFINITY;
            clone.bestMacroAvgF = Double.NEGATIVE_INFINITY;
            clone.bestMacroAvgPPV = Double.NEGATIVE_INFINITY;
            clone.bestExpression = null;
            clone.bestSolution = null;
        } catch (IOException ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        return clone;
    }
    
    public ParkinsonAdaBoostClassifier(Properties properties) throws IOException {
        super(properties.getProperty("BnfPathFile"), 1);
        this.properties = properties;
        this.threadId = CURRENT_THREAD_ID++;
        compiler = new MyCompiler(properties);
        
        // Get the classifier and the evaluator of metrics
        kindClassifier = properties.getProperty("Classifier");
        switch (kindClassifier) {
            case "quantizer":
                classifier = new Quantizer(kindClassifier, Integer.valueOf(properties.getProperty("MaxPDLevel")));
                classifierEval = new ClassifierEvaluator(Integer.valueOf(properties.getProperty("MaxPDLevel"))+1);
                break;
            case "dichotomizer":
                classifier = new Quantizer(kindClassifier, 1);
                classifierEval = new ClassifierEvaluator(2);
                break;
        }
    }
    
    
    @Override
    public void evaluate(Solutions<Variable<Integer>> solutions) {
        StringBuilder currentJavaFile = new StringBuilder();
        int numOfIncorrectSolutions = 0;
        
        currentJavaFile.append("import jeco.util.Maths;\n");
        currentJavaFile.append("import jeco.util.FastFourierTransformer;\n");
        currentJavaFile.append("import jeco.util.string.stringManagement;\n");
        
        
        currentJavaFile.append("public class PopEvaluator").append(threadId).append(" extends jeco.operator.evaluator.AbstractPopEvaluator {\n\n");
        
        /**
         * Implementation of functions of the Grammar for discrete Features
         * */
        /* Min */
        currentJavaFile.append("public double MyFeatMin(double a, double b) {\n");
        currentJavaFile.append("\tdouble[] data = new double[2];\n");
        currentJavaFile.append("\tdata[0] = a;\n");
        currentJavaFile.append("\tdata[1] = b;\n");
        currentJavaFile.append("\treturn Maths.min(data);\n");
        currentJavaFile.append("\t}\n");
        
        currentJavaFile.append("public double MyFeatMin(String arrayS) {\n");
        currentJavaFile.append("\treturn Maths.min(stringToArray(arrayS));\n");
        currentJavaFile.append("\t}\n");
        
        /* MX */
        currentJavaFile.append("public double MyFeatMax(double a, double b) {\n");
        currentJavaFile.append("\tdouble[] data = new double[2];\n");
        currentJavaFile.append("\tdata[0] = a;\n");
        currentJavaFile.append("\tdata[1] = b;\n");
        currentJavaFile.append("\treturn Maths.max(data);\n");
        currentJavaFile.append("\t}\n");
        
        currentJavaFile.append("public double MyFeatMax(String arrayS) {\n");
        currentJavaFile.append("\treturn Maths.max(stringToArray(arrayS));\n");
        currentJavaFile.append("\t}\n");
        
        /* Sum */
        currentJavaFile.append("public double MyFeatSum(double a, double b) {\n");
        currentJavaFile.append("\tdouble[] data = new double[2];\n");
        currentJavaFile.append("\tdata[0] = a;\n");
        currentJavaFile.append("\tdata[1] = b;\n");
        currentJavaFile.append("\treturn Maths.sum(data);\n");
        currentJavaFile.append("\t}\n");
        
        currentJavaFile.append("public double MyFeatSum(String arrayS) {\n");
        currentJavaFile.append("\treturn Maths.sum(stringToArray(arrayS));\n");
        currentJavaFile.append("\t}\n");
        
        
        /* Average */
        currentJavaFile.append("public double MyFeatAvg(double a, double b) {\n");
        currentJavaFile.append("\tdouble[] data = new double[2];\n");
        currentJavaFile.append("\tdata[0] = a;\n");
        currentJavaFile.append("\tdata[1] = b;\n");
        currentJavaFile.append("\treturn Maths.mean(data);\n");
        currentJavaFile.append("\t}\n");
        
        currentJavaFile.append("public double MyFeatAvg(String arrayS) {\n");
        currentJavaFile.append("\treturn Maths.mean(stringToArray(arrayS));\n");
        currentJavaFile.append("\t}\n");
        
        /* Std */
        currentJavaFile.append("public double MyFeatStd(double a, double b) {\n");
        currentJavaFile.append("\tdouble[] data = new double[2];\n");
        currentJavaFile.append("\tdata[0] = a;\n");
        currentJavaFile.append("\tdata[1] = b;\n");
        currentJavaFile.append("\treturn Maths.std(data);\n");
        currentJavaFile.append("\t}\n");
        
        currentJavaFile.append("public double MyFeatStd(String arrayS) {\n");
        currentJavaFile.append("\treturn Maths.std(stringToArray(arrayS));\n");
        currentJavaFile.append("\t}\n");
        
        
        /* Pow */
        currentJavaFile.append("public double MyPow(double a, double pow) {\n");
        currentJavaFile.append("\treturn Math.pow(a, pow);\n");
        currentJavaFile.append("\t}\n");
        
        currentJavaFile.append("public double MyFeatSum(String s, double pow) {\n");
        currentJavaFile.append("\treturn Math.pow(stringToArray(s)[0], pow);\n");
        currentJavaFile.append("\t}\n");
        
        
        /**
         * Utils
         * */
        currentJavaFile.append("public double raw(String arrayS){\n");
        currentJavaFile.append("\treturn stringToArray(arrayS)[0];\n");
        currentJavaFile.append("}\n");
        
        
        currentJavaFile.append("public double[] stringToArray(String s){\n");
        currentJavaFile.append("\tString[] arrayElem = s.split(\";\");\n");
        currentJavaFile.append("\tdouble[] data = new double[arrayElem.length];\n");
        currentJavaFile.append("\tfor (int j = 0; j < arrayElem.length; j++) {\n");
        currentJavaFile.append("\t\tdata[j] = stringManagement.isInteger(arrayElem[j]) ? getDataTable(Integer.valueOf(arrayElem[j]), -1) : getDataTable(arrayElem[j], -1);\n");
        //currentJavaFile.append("\t\tdata[j] = getDataTable(Integer.valueOf(arrayElem[j]), -1);\n");
        currentJavaFile.append("\t}\n");
        currentJavaFile.append("\treturn data;\n");
        currentJavaFile.append("\t}\n");
        
        currentJavaFile.append("\tpublic double[] getFeatures(int a, int b) {\n");
        currentJavaFile.append("\tint min = 0;\n");
        currentJavaFile.append("\tint max = 0;\n");
        currentJavaFile.append("\tif (a >= b) {max = a; min = b;} else {max = b; min = a;};\n");
        currentJavaFile.append("\tdouble[] data = new double[max-min+1];\n");
        currentJavaFile.append("\tfor(int i=0; i<=max-min; i++){\n");
        currentJavaFile.append("\tdata[i] = getDataTable(min+i, -1);\n");
        currentJavaFile.append("\t}\n");
        currentJavaFile.append("\treturn data;\n");
        currentJavaFile.append("\t}\n");
        
        currentJavaFile.append("\tpublic void evaluateExpression(int idxExpr) {\n");
        currentJavaFile.append("\t\treturn;\n");
        currentJavaFile.append("\t}\n\n");
        
        currentJavaFile.append("\tpublic double evaluate(int idxExpr, int k) {\n");
        currentJavaFile.append("\t\tdouble result = 0.0;\n");
        currentJavaFile.append("\t\ttry {\n");
        
        currentJavaFile.append("\t\t\tswitch(idxExpr) {\n");
        numOfIncorrectSolutions = 0;
        for (int i = 0; i < solutions.size(); ++i) {
            currentJavaFile.append("\t\t\t\tcase ").append(i).append(":\n");
            Solution<Variable<Integer>> solution = solutions.get(i);
            Phenotype phenotype = generatePhenotype(solution);
            if (correctSol) {
                currentJavaFile.append("\t\t\t\t\tresult = ").append(phenotype.toString()).append(";\n");
            } else {
                numOfIncorrectSolutions += 1;
                currentJavaFile.append("\t\t\t\t\tresult = Double.POSITIVE_INFINITY;\n");
            }
            currentJavaFile.append("\t\t\t\t\tbreak;\n");
        }
        currentJavaFile.append("\t\t\t\tdefault:\n");
        currentJavaFile.append("\t\t\tSystem.err.println(\"GE result is DefaultValue.\");\n");
        currentJavaFile.append("\t\t\t\t\tresult = Double.POSITIVE_INFINITY;\n");
        currentJavaFile.append("\t\t\t}\n"); // End switch
        
        logger.finest("incorrect_sols," + numOfIncorrectSolutions);
        
        currentJavaFile.append("\t\t}\n"); // End try
        currentJavaFile.append("\t\tcatch (Exception ee) {\n");
        currentJavaFile.append("\t\t\tSystem.err.println(ee.fillInStackTrace());\n");
        //currentJavaFile.append("\t\t\tSystem.err.println(\"Exception trying to calculate the GE result.\");\n");
        currentJavaFile.append("\t\t\tresult = Double.NaN;\n");
        currentJavaFile.append("\t\t}\n"); // End catch
        currentJavaFile.append("\t\tif(Double.isNaN(result)) {\n");
        //currentJavaFile.append("\t\t\tSystem.err.println(\"GE result is NaN.\");\n");
        currentJavaFile.append("\t\t}\n");
        currentJavaFile.append("\t\treturn result;\n");
        currentJavaFile.append("\t}\n");
        currentJavaFile.append("}\n");
        // Compilation process:
        try {
            File file = new File(compiler.getWorkDir() + File.separator + "PopEvaluator" + threadId + ".java");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(currentJavaFile.toString());
            writer.flush();
            writer.close();
            LinkedList<String> filePaths = new LinkedList<>();
            filePaths.add(file.getAbsolutePath());
            boolean sucess = compiler.compile(filePaths);
            if (!sucess) {
                logger.severe("Unable to compile, with errors:");
                logger.severe(compiler.getOutput());
            }
        } catch (Exception ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        
        // For each folding apply the solutions.
        // Evaluate all the solutions with the compiled file.
        evaluator = null;
        try {
            evaluator = (AbstractPopEvaluator) (new MyLoader(compiler.getWorkDir())).loadClass("PopEvaluator" + threadId).newInstance();
        } catch (Exception ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        
        // Load all the features of all the patients
        evaluator.setFeaturesNames(featuresTable.getFeaturesTable("names"));
        
        // For each solution
        for (int s = 0; s < solutions.size(); ++s) {
            Solution<Variable<Integer>> solution = solutions.get(s);
            classifierEval.resetConfusionMatrix();
            //logger.info("Solución: " + generatePhenotype(solution).toString());
            
            computeFolds(evaluator, solution, s, currentData);
            
            double cr = classifierEval.getClassificationRate();
            double macroPPV = classifierEval.getMacroAveragePrecision();
            double macroTPR = classifierEval.getMacroAverageSensitivity();
            double macroTNR = classifierEval.getMacroAverageSpecificity();
            double macroFvalue = classifierEval.getMacroFValue();
            
            // Return the value to the algorithm:
            solution.getObjectives().set(0, 1-macroFvalue); //(1-macroFvalue) to maximize the F-value
            
            if (macroFvalue > bestMacroAvgF) {
                bestSolution = solution;
                bestSolIdx = s;
                bestExpression = generatePhenotype(solution).toString();
                bestClassRate = cr;
                bestMacroAvgTPR = macroTPR;
                bestMacroAvgTNR = macroTNR;
                bestMacroAvgPPV = macroPPV;
                bestMacroAvgF = macroFvalue;
                logger.info("BEST FOUND, Thread-Id: " + threadId + ", Macro F-value=" + (100*macroFvalue) + "; Expresion=" + bestExpression);
                
                bestResultsMatrix = resultsMatrix;
            }
        }
    }
    
    
    @Override
    public void evaluate(Solution<Variable<Integer>> solution) {
        logger.severe("The solutions should be already evaluated. You should not see this message.");
    }
    
    @Override
    public void evaluate(Solution<Variable<Integer>> solution, Phenotype phenotype) {
        logger.severe("The solutions should be already evaluated. You should not see this message.");
    }
    
    
    
    public void computeFolds(AbstractPopEvaluator evaluator, Solution<Variable<Integer>> solution, int solIdx, int[][] data) {
        int it = 0;
        resultsMatrix = new double[data.length*data[0].length][2];
        
        // For every patient apply the solution
        for (int[] folds1 : data) {
            for (int j = 0; j < data[0].length; j++) {
                int p = folds1[j];
                
                //System.out.println("Patient: GA" + (int)featuresTable.table.get(p)[IDCol]);
                evaluator.setDataTable((ArrayList<double[]>) featuresTable.getFeaturesTable(p, p+1));
                
                // Compute and classify GE:
                double resultGE = evaluator.evaluate(solIdx, -1);
                int originalValue = 0;
                int qResult = 0;
                
                switch (kindClassifier) {
                    case "quantizer":
                        originalValue = (int)featuresTable.getFeaturesTable("features").get(p)[pdLevelCol];
                        break;
                    case "dichotomizer":
                        originalValue = ((int)featuresTable.getFeaturesTable("features").get(p)[pdLevelCol] > 0) ? 1 : 0;
                        break;
                }
                
                resultsMatrix[it][0] = resultGE;
                resultsMatrix[it++][1] = originalValue;
                
                
                if (!Double.isNaN(resultGE)){
                    qResult = classifier.getQ(resultGE);
                } else {
                    // Store as a misclassification (max difference). This
                    // happens mostly when an exercise is not available for a patient.
                    switch (kindClassifier) {
                        case "dichotomizer":
                            if (originalValue == 1) {
                                qResult = 0;
                            } else {
                                qResult = 1;
                            }
                            if (whoWas) {
                                logger.info("NaN result for patient GA" +  (int)featuresTable.getFeaturesTable("features").get(p)[IDCol]);
                            }
                            break;
                    }
                }
                classifierEval.setValue(j, originalValue, qResult, 1);
                
                if ((originalValue != qResult) && whoWas) {
                    logger.info("Misclassification of patient GA" +  (int)featuresTable.getFeaturesTable("features").get(p)[IDCol] + " . Original: " + originalValue + ", resultGE: " + qResult);
                }
            }
        }
    }
    
    
    public static Properties loadProperties(String propertiesFilePath) {
        Properties properties = new Properties();
        try {
            properties.load(new BufferedReader(new FileReader(new File(propertiesFilePath))));
            File clsDir = new File(properties.getProperty("WorkDir"));
            URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<URLClassLoader> sysclass = URLClassLoader.class;
            Method method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(sysloader, new Object[]{clsDir.toURI().toURL()});
        } catch (Exception ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        return properties;
    }
    
    public int[][] getTrainingFolds(int[][] patientsIdxs, int currentFolding) {
        int[][] trainingFolds = new int[patientsIdxs.length-1][patientsIdxs[0].length];
        
        // Leave one group out
        int rNor = 0;
        for (int r=0; r<patientsIdxs.length; r++) {
            if ( r != currentFolding) {
                trainingFolds[rNor++] = patientsIdxs[r];
            }
        }
        return trainingFolds;
    }
    
    public int[][] getValidationFold(int[][] patientsIdxs, int currentFolding) throws IOException {
        int[][] validationFold = new int[1][patientsIdxs[0].length];
        if (properties.getProperty("readExternalFile").equals("yes")){
            validationFold = featuresTable.getPatientsIdXs(properties.getProperty("validationFile"));
        } else {
            validationFold[0] = patientsIdxs[currentFolding];
        }
        return validationFold;
    }
    
    
    public void loadData(String type) throws IOException{
        featuresTable = new FeaturesTable(properties, type, Integer.valueOf(properties.getProperty("IdxBegin", "-1")), Integer.valueOf(properties.getProperty("IdxEnd", "-1")));
        // Get the clinical information
        pdLevelCol = Integer.valueOf(properties.getProperty("PDLevelCol"));
        IDCol = Integer.valueOf(properties.getProperty("IDCol"));
    }
    
    public void runLearner(ArrayList<double[]> wData) throws IOException{
        // Set weighted data
        featuresTable.table = wData;
        
        // Variables to store the results:
        double[] classRateAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
        double[] macroSensitivityAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
        double[] macroSpecificityAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
        double[] macroPrecisionAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
        String[] expressionAllFolds = new String[Integer.valueOf(properties.getProperty("N"))];
        double[] macroFValueAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
        double[] sensitivityAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
        double[] specificityAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
        double[] precisionAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
        double[] fValueAllFolds = new double[Integer.valueOf(properties.getProperty("N"))];
        
        // Call optimization problem:
        IntegerFlipMutation<Variable<Integer>> mutationOperator = new IntegerFlipMutation<>(this, 1.0 / reader.getRules().size());
        SinglePointCrossover<Variable<Integer>> crossoverOperator = new SinglePointCrossover<>(this, SinglePointCrossover.DEFAULT_FIXED_CROSSOVER_POINT, SinglePointCrossover.DEFAULT_PROBABILITY, SinglePointCrossover.AVOID_REPETITION_IN_FRONT);
        SimpleDominance<Variable<Integer>> comparator = new SimpleDominance<>();
        BinaryTournament<Variable<Integer>> selectionOp = new BinaryTournament<>(comparator);
        SimpleGeneticAlgorithm<Variable<Integer>> algorithm = new SimpleGeneticAlgorithm<>(this, Integer.valueOf(properties.getProperty("NumIndividuals")), Integer.valueOf(properties.getProperty("NumGenerations")), true, mutationOperator, crossoverOperator, selectionOp);
        
        Solutions<Variable<Integer>> popAfterExecution = new Solutions<>();
        switch (properties.getProperty("Parallelization")) {
            case "yes":
                MasterWorkerThreads<Variable<Integer>> masterWorker = new MasterWorkerThreads<>(algorithm, this, Integer.valueOf(properties.getProperty("NumCores")));
                popAfterExecution = masterWorker.execute();
                break;
            default:
                algorithm.initialize();
                popAfterExecution = algorithm.execute();
        }
        
        // Take the best of all threads:
        Solution<Variable<Integer>> bestSolution = popAfterExecution.get(0);
        
        // Reset everything:
        classifierEval.resetConfusionMatrix();
        bestClassRate = Double.NEGATIVE_INFINITY;
        bestMacroAvgTPR = Double.NEGATIVE_INFINITY;
        bestMacroAvgTNR = Double.NEGATIVE_INFINITY;
        bestMacroAvgF = Double.NEGATIVE_INFINITY;
        bestMacroAvgPPV = Double.NEGATIVE_INFINITY;
        bestResultsMatrix = null;
        
        // Validate the best function with the holded fold
        // This is the result of the training of this folder
        currentData = getValidationFold(featuresTable.getPatientsIdXs(true), CURRENT_THREAD_ID);
        
        // Track misclassifications:
        whoWas = false;
        
        // Evaluate the hold folding with the best solution found (each thread):
        Solutions<Variable<Integer>> tempSolutions = new Solutions<>();
        tempSolutions.add(bestSolution);
        evaluate(tempSolutions);
        
        // Store the result of the training with this fold:
        macroFValueAllFolds[CURRENT_THREAD_ID] = classifierEval.getMacroFValue();
        classRateAllFolds[CURRENT_THREAD_ID] = classifierEval.getClassificationRate();
        macroSensitivityAllFolds[CURRENT_THREAD_ID] = classifierEval.getMacroAverageSensitivity();
        macroSpecificityAllFolds[CURRENT_THREAD_ID] = classifierEval.getMacroAverageSpecificity();
        macroPrecisionAllFolds[CURRENT_THREAD_ID] = classifierEval.getMacroAveragePrecision();
        expressionAllFolds[CURRENT_THREAD_ID] = generatePhenotype(bestSolution).toString();
        fValueAllFolds[CURRENT_THREAD_ID] = classifierEval.getFValue(1);
        sensitivityAllFolds[CURRENT_THREAD_ID] = classifierEval.getSensitivity(1);
        specificityAllFolds[CURRENT_THREAD_ID] = classifierEval.getSpecificity(1);
        precisionAllFolds[CURRENT_THREAD_ID] = classifierEval.getPrecision(1);
        
        logger.info("validationOfFold,averageAllClasses," + CURRENT_THREAD_ID + "," + (100*macroFValueAllFolds[CURRENT_THREAD_ID]) + "," + (100*classRateAllFolds[CURRENT_THREAD_ID]) +  "," + (100*macroPrecisionAllFolds[CURRENT_THREAD_ID]) + "," + 100*(macroSensitivityAllFolds[CURRENT_THREAD_ID]));
        logger.info("validationOfFold,averageClass1," + CURRENT_THREAD_ID + "," + (100*fValueAllFolds[CURRENT_THREAD_ID]) + "," + (100*classRateAllFolds[CURRENT_THREAD_ID]) +  "," + (100*precisionAllFolds[CURRENT_THREAD_ID]) + "," + 100*(sensitivityAllFolds[CURRENT_THREAD_ID]));
        
        // Print the confussion matrix
        int[][] cf = classifierEval.getConfusionMatrix();
        logger.info("Confussion Matrix:");
        logger.info("     |F|T|");
        logger.info("     |---|");
        logger.info("F_GE |" + cf[0][0] + "|" + cf[0][1] + "|");
        logger.info("     |---|");
        logger.info("T_GE |" + cf[1][0] + "|" + cf[1][1] + "|");
        logger.info("     |---|");
        
        whoWas = false;
        
        // Finally calculate the final expression, result of training (OUT OF THE IF)
        
        // Get metrics from training:
        logger.info("TRAINING,averageAllClasses," + (100*Maths.mean(macroFValueAllFolds)) + "," + (100*Maths.std(macroFValueAllFolds)) + "," + (100*Maths.mean(classRateAllFolds)) + "," + (100*Maths.std(classRateAllFolds)) + "," + (100*Maths.mean(macroSensitivityAllFolds)) +  "," + (100*Maths.std(macroSensitivityAllFolds)) + "," + (100*Maths.mean(macroSpecificityAllFolds)) + "," + (100*Maths.std(macroSpecificityAllFolds)) + "," + (100*Maths.mean(macroPrecisionAllFolds)) + "," + (100*Maths.std(macroPrecisionAllFolds)));
        logger.info("TRAINING,averageClass1," + (100*Maths.mean(fValueAllFolds)) + "," + (100*Maths.std(fValueAllFolds)) + "," + (100*Maths.mean(classRateAllFolds)) + "," + (100*Maths.std(classRateAllFolds)) + "," + (100*Maths.mean(sensitivityAllFolds)) +  "," + (100*Maths.std(sensitivityAllFolds)) + "," + (100*Maths.mean(specificityAllFolds)) + "," + (100*Maths.std(specificityAllFolds)) + "," + (100*Maths.mean(precisionAllFolds)) + "," + (100*Maths.std(precisionAllFolds)));
    }
    
    public void runAdaBoost() throws IOException{
        AdaBoost myAdaBoost = new AdaBoost(this, classifierEval, featuresTable.table, 10);
        myAdaBoost.run();
        if (true){
            return;
        }       
    }
    
    public static void main(String[] args) throws IOException {
        String propertiesFilePath = "test" + File.separator + ParkinsonAdaBoostClassifier.class.getSimpleName() + ".properties";
        
        if (args.length == 1) {
            propertiesFilePath = args[0];
        } else if (args.length >= 2) {
            propertiesFilePath = args[0];
        }
        
        try {
            // TRAINING
            Properties properties = loadProperties(propertiesFilePath);
            HeroLogger.setup(properties.getProperty("LoggerBasePath") + ".log", Level.parse(properties.getProperty("LoggerLevel")));
            
            /////////////////////////////////////////
            ParkinsonAdaBoostClassifier problem;
            // If N-fold cross-validation: first run it and calculate metrics.
            if ("yes".equals(properties.getProperty("NFoldCrossVal"))) {
                // For each fold
                
                for (int i=0; i<Integer.valueOf(properties.getProperty("N")); i++){
                    CURRENT_THREAD_ID = i;
                
                    if (Integer.valueOf(properties.getProperty("N")) > 1){
                        logger.info("Starting Folding Num: " + i);
                    } else {
                        logger.info("Starting the only fold...");
                    }
                    // New problem and new algorihm for each fold:
                    problem = new ParkinsonAdaBoostClassifier(properties);
                    problem.loadData("training");
                    
                    // Select the current fold
                    if (problem.properties.getProperty("readExternalFile").equals("yes")){
                        problem.currentData = problem.featuresTable.getPatientsIdXs(problem.properties.getProperty("trainingFile"));
                    } else {
                        problem.currentData = problem.getTrainingFolds(problem.featuresTable.getPatientsIdXs(true), i);
                    }
                    
                    // Call AdaBoost
                    problem.runAdaBoost();
                }
            }
            
            // FINAL TRAINING. Use all the data:
            if (properties.getProperty("trainingAllPatients").equals("yes")) {
                
                // New problem and new algorihm to compute all the patients:
                problem = new ParkinsonAdaBoostClassifier(properties);
                problem.loadData("training");
                problem.classifierEval.resetConfusionMatrix();
                
                // Track misclassifications:
                whoWas = false;
                
                // Select all the patients:        
                problem.currentData = problem.featuresTable.getPatientsIdXs(false);

                
                IntegerFlipMutation<Variable<Integer>> mutationOperator = new IntegerFlipMutation<>(problem, 1.0 / problem.reader.getRules().size());
                SinglePointCrossover<Variable<Integer>> crossoverOperator = new SinglePointCrossover<>(problem, SinglePointCrossover.DEFAULT_FIXED_CROSSOVER_POINT, SinglePointCrossover.DEFAULT_PROBABILITY, SinglePointCrossover.AVOID_REPETITION_IN_FRONT);
                SimpleDominance<Variable<Integer>> comparator = new SimpleDominance<>();
                BinaryTournament<Variable<Integer>> selectionOp = new BinaryTournament<>(comparator);
                SimpleGeneticAlgorithm<Variable<Integer>> algorithm = new SimpleGeneticAlgorithm<>(problem, Integer.valueOf(properties.getProperty("NumIndividuals")), Integer.valueOf(properties.getProperty("NumGenerations")), true, mutationOperator, crossoverOperator, selectionOp);
                
                // Call optimization problem:
                Solutions<Variable<Integer>> popAfterExecution = new Solutions<>();
                
                switch (properties.getProperty("Parallelization")) {
                    case "yes":
                        MasterWorkerThreads<Variable<Integer>> masterWorker = new MasterWorkerThreads<>(algorithm, problem, Integer.valueOf(properties.getProperty("NumCores")));
                        popAfterExecution = masterWorker.execute();
                        break;
                    default:
                        algorithm.initialize();
                        popAfterExecution = algorithm.execute();
                }
                
                // Take the best solution:
                Solution<Variable<Integer>> bestSolution = popAfterExecution.get(0);
                String bestExpression = problem.generatePhenotype(bestSolution).toString();
                
                // Evaluate the best solution found (all threads):
                Solutions<Variable<Integer>> tempSolutions = new Solutions<>();
                tempSolutions.add(bestSolution);
                problem.evaluate(tempSolutions);
                
                logger.info("Final Training...");
                switch (problem.kindClassifier) {
                    case "dichotomizer":
                        // Print the confussion matrix
                        int[][] cf = problem.classifierEval.getConfusionMatrix();
                        logger.info("Confussion Matrix:");
                        logger.info("     |F|T|");
                        logger.info("     |---|");
                        logger.info("F_GE |" + cf[0][0] + "|" + cf[0][1] + "|");
                        logger.info("     |---|");
                        logger.info("T_GE |" + cf[1][0] + "|" + cf[1][1] + "|");
                        logger.info("     |---|");
                        logger.info("FINAL_TRAINING,PD class 1," + (100*problem.classifierEval.getFValue(1)) + "," + (100*problem.classifierEval.getClassificationRate()) +  "," + (100*problem.classifierEval.getPrecision(1)) + "," + 100*(problem.classifierEval.getSensitivity(1)) + "," + 100*(problem.classifierEval.getSpecificity(1)));
                        break;
                }
                logger.info("FINAL_TRAINING,All classes," + (100*problem.classifierEval.getMacroFValue()) + "," + (100*problem.classifierEval.getClassificationRate()) +  "," + (100*problem.classifierEval.getMacroAveragePrecision()) + "," + 100*(problem.classifierEval.getMacroAverageSensitivity()) + "," + 100*(problem.classifierEval.getMacroAverageSpecificity()) + "," + bestExpression);
                logger.info("...final training done");
                
                /* Print resutlsGE VS real values of the training*/
                //logger.info("resultsGE,realValue");
                //for (int y=0; y<problem.bestResultsMatrix.length; y++){
                //    logger.info(problem.bestResultsMatrix[y][0] + "," + problem.bestResultsMatrix[y][1]);
                ///}
                
                ////////////////////////////////////////////////////////////////////
                // TEST
                // Take the solution found with all the patients. Evaluate over the test data-set:
                logger.info("TEST:");
                
                // Track misclassifications:
                whoWas = true;
                
                problem = new ParkinsonAdaBoostClassifier(properties);
                problem.loadData("test");
                problem.classifierEval.resetConfusionMatrix();
                
                // Select all the patients:
                problem.currentData = problem.featuresTable.getPatientsIdXs(false);
                
                // Evaluate the best solution found (all threads):
                problem.evaluate(tempSolutions);
                
                
                switch (problem.kindClassifier) {
                    case "dichotomizer":
                        // Print the confussion matrix
                        int[][] cf = problem.classifierEval.getConfusionMatrix();
                        logger.info("Confussion Matrix:");
                        logger.info("     |F|T|");
                        logger.info("     |---|");
                        logger.info("F_GE |" + cf[0][0] + "|" + cf[0][1] + "|");
                        logger.info("     |---|");
                        logger.info("T_GE |" + cf[1][0] + "|" + cf[1][1] + "|");
                        logger.info("     |---|");
                        logger.info("FINAL_TEST,PD class 1," + (100*problem.classifierEval.getFValue(1)) + "," + (100*problem.classifierEval.getClassificationRate()) +  "," + (100*problem.classifierEval.getPrecision(1)) + "," + 100*(problem.classifierEval.getSensitivity(1)) + "," + 100*(problem.classifierEval.getSpecificity(1)));
                        break;
                }
                logger.info("FINAL_TEST,All classes," + (100*problem.classifierEval.getMacroFValue()) + "," + (100*problem.classifierEval.getClassificationRate()) +  "," + (100*problem.classifierEval.getMacroAveragePrecision()) + "," + 100*(problem.classifierEval.getMacroAverageSensitivity()) + "," + 100*(problem.classifierEval.getMacroAverageSpecificity()) + "," + bestExpression);
            }
        } catch (IOException ex) {
            Logger.getLogger(ParkinsonAdaBoostClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
