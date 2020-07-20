/**
 * LEOSS Data Release
 * Copyright (C) 2020 - LEOSS
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
 */
package net.leoss.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.deidentifier.arx.Data;
import org.deidentifier.arx.exceptions.RollbackRequiredException;

/**
 * Main entry point
 * @author Fabian Prasser
 * @author Florian Kohlmayer
 */
public class Main {

    /**
     * Main entry point
     * @param args
     * @throws IOException 
     * @throws RollbackRequiredException 
     */
    public static void main(String[] args) throws IOException, RollbackRequiredException {
        
        // Check
        if (args == null || args.length < 2 || args[0] == null || args[0].length() == 0 || args[1] == null || args[1].length() == 0) {
            throw new IllegalArgumentException("You need to specify files for input and output.");
        }
        File input = new File(args[0]);
        if (!input.exists()) {
            throw new IllegalArgumentException("The specified input file doesn't exist.");
        }
        
        // Check output
        if (!args[1].endsWith(".csv")) {
            args[1] = args[1] + ".csv";
        }
        
        // Create empty output file
        File output = new File(args[1]);
        output.createNewFile();
        if (!Files.isWritable(new File(args[1]).toPath())) {
            throw new IllegalArgumentException("The specified output file isn't writable.");
        }
        
        // Parse
        Data data = IO.loadData(input);
        
        // Generalize
        data = Anon.anonymizeGeneralize(data);

        // Anonymize
        data = Anon.anonymizeFirstStage(data);
        data = Anon.anonymizeSecondStage(data);

        // Report
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(output.getAbsolutePath() + ".report")));
        for (Report report : Report.getReports()) {
            writer.write(report.toString());
            writer.write("\n");
        }
        writer.close();
        
        // Write
        IO.writeOutput(data, output);
    }
}
