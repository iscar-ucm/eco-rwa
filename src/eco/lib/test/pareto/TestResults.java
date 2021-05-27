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
package eco.lib.test.pareto;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import eco.core.operator.comparator.SolutionDominance;
import eco.core.problem.Solution;
import eco.core.problem.Solutions;
import eco.core.problem.Variable;

/**
 *
 * @author José Luis Risco Martín
 */
public class TestResults {

    protected String resultsPath;

    public TestResults(String resultsPath) {
        this.resultsPath = resultsPath;
    }

    public void reduce() {
        for (int no = 2; no < 5; no++) { // Number of objectives
            for (int nt = 0; nt < 30; nt++) { // Number of tests
                // 1) Generate initial population:
                Solutions<Variable<Integer>> solutions = new Solutions<>();
                for (int i = 0; i < 1000; ++i) {
                    Solution<Variable<Integer>> solI = new Solution<>(no);
                    for (int j = 0; j < 20; ++j) {
                        Variable<Integer> varJ = new Variable<>(0);
                        solI.getVariables().add(varJ);
                    }
                    for (int j=0; j<no; j++) {
                        solI.getObjectives().set(j, no*Math.random());
                    }
                    solutions.add(solI);
                }
                // 2) Save the whole population:
                this.save(solutions, "all", no, nt+1);
                // 3) Reduce to non-dominated and save
                solutions.reduceToNonDominated(new SolutionDominance<>());
                this.save(solutions, "reduced", no, nt+1);
            }
        }
    }
    
    public void save(Solutions<Variable<Integer>> solutions, String prefix, int no, int nt) {
        String filePath = resultsPath + File.separator + "population-" + prefix + "-" + no + "-" + nt + ".csv";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath)));
            for(Solution<Variable<Integer>> solution : solutions) {
                ArrayList<Variable<Integer>> variables = solution.getVariables();
                for(Variable<Integer> variable : variables) {
                    writer.write(variable.getValue().toString() + ";");
                }
                for(int i = 0; i<solution.getObjectives().size(); ++i) {
                    if(i>0) {
                        writer.write(";");                        
                    }
                    writer.write(solution.getObjective(i).toString());
                }
                writer.write("\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(TestResults.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        TestResults test = new TestResults("D:\\jlrisco\\Borrar\\hero");
        test.reduce();
    }
}
