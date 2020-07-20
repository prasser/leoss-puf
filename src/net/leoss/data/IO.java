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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.DataSource;
import org.deidentifier.arx.DataType;
import org.deidentifier.arx.io.CSVDataOutput;

/**
 * Data loading
 * @author Fabian Prasser
 * @author Florian Kohlmayer
 */
public class IO {

    /** Not available */
    public static final String VALUE_NA                                 = "n/a";
    /** Not available */
    public static final String VALUE_UNKNOWN_MISSING                    = "unknown/missing";

    /** Final field */
    public static final String FIELD_AGE                                = "Age.at.diagnosis";
    /** Final field */
    public static final String FIELD_GENDER                             = "Sex";
    /** Final field */
    public static final String FIELD_DIAGNOSIS_MONTH                    = "Month.first.diagnosis";
    /** Final field */
    public static final String FIELD_DIAGNOSIS_YEAR                     = "Year.first.diagnosis";
    /** Final field */
    public static final String FIELD_PHASE_UNCOMPLICATED                = "Uncomplicated.phase";
    /** Final field */
    public static final String FIELD_PHASE_COMPLICATED                  = "Complicated.phase";
    /** Final field */
    public static final String FIELD_PHASE_CRITICAL                     = "Critical.phase";
    /** Final field */
    public static final String FIELD_PHASE_RECOVERY                     = "Recovery.phase";
    /** Final field */
    public static final String FIELD_LAST_KNOWN_STATUS                  = "Last.known.patient.status";
    /** Final field */
    public static final String FIELD_PHASE_COMPLICATED_VASSOPRESSORS    = "Vasopressors.in.complicated.phase";
    /** Final field */
    public static final String FIELD_PHASE_CRITICIAL_VASSOPRESSORS      = "Vasopressors.in.critical.phase";
    /** Final field */
    public static final String FIELD_PHASE_CRITICIAL_VENTILATION        = "Invasive.ventilation.in.critical.phase";
    /** Final field */
    public static final String FIELD_PHASE_UNCOMPLICATED_SUPERINFECTION = "Superinfection.in.uncomplicated.phase";
    /** Final field */
    public static final String FIELD_PHASE_COMPLICATED_SUPERINFECTION   = "Superinfection.in.complicated.phase";
    /** Final field */
    public static final String FIELD_PHASE_CRITICIAL_SUPERINFECTION     = "Superinfection.in.critical.phase";
    /** Final field */
    public static final String FIELD_PHASE_RECOVERY_SYMPTOMS            = "Symptoms.in.recovery.phase";

    /** Legacy field */
    public static final String LEGACY_FIELD_DIAGNOSIS_MONTH_YEAR        = "Month.year.first.diagnosis";

    /**
     * File loading
     * @param inputFile
     * @return
     * @throws IOException 
     */
    public static Data loadData(File inputFile) throws IOException {
        
        // Import process
        DataSource sourceSpecification = DataSource.createCSVSource(inputFile, StandardCharsets.UTF_8, ';', true);
        
        // Clean columns
        sourceSpecification.addColumn(0, FIELD_AGE, DataType.STRING);
        sourceSpecification.addColumn(1, FIELD_GENDER, DataType.STRING);
        sourceSpecification.addColumn(2, LEGACY_FIELD_DIAGNOSIS_MONTH_YEAR, DataType.STRING);
        sourceSpecification.addColumn(3, FIELD_PHASE_UNCOMPLICATED, DataType.STRING);
        sourceSpecification.addColumn(4, FIELD_PHASE_COMPLICATED, DataType.STRING);
        sourceSpecification.addColumn(5, FIELD_PHASE_CRITICAL, DataType.STRING);
        sourceSpecification.addColumn(6, FIELD_PHASE_RECOVERY, DataType.STRING);
        sourceSpecification.addColumn(7, FIELD_LAST_KNOWN_STATUS, DataType.STRING);
        sourceSpecification.addColumn(8, FIELD_PHASE_COMPLICATED_VASSOPRESSORS, DataType.STRING);
        sourceSpecification.addColumn(9, FIELD_PHASE_CRITICIAL_VASSOPRESSORS, DataType.STRING);
        sourceSpecification.addColumn(10, FIELD_PHASE_CRITICIAL_VENTILATION, DataType.STRING);
        sourceSpecification.addColumn(11, FIELD_PHASE_UNCOMPLICATED_SUPERINFECTION, DataType.STRING);
        sourceSpecification.addColumn(12, FIELD_PHASE_COMPLICATED_SUPERINFECTION, DataType.STRING);
        sourceSpecification.addColumn(13, FIELD_PHASE_CRITICIAL_SUPERINFECTION, DataType.STRING);
        sourceSpecification.addColumn(14, FIELD_PHASE_RECOVERY_SYMPTOMS, DataType.STRING);
        
        // Load input file
        DataHandle inputHandle = Data.create(sourceSpecification).getHandle();
        
        // Convert
        return convert(inputHandle);
    }
    
    /**
     * Writes the data, shuffles rows
     * @param result 
     * @param output
     * @throws IOException 
     */
    public static void writeOutput(Data result, File output) throws IOException {
        CSVDataOutput writer = new CSVDataOutput(output, ';');
        writer.write(result.getHandle().iterator());
    }

    /**
     * Convert handle to iterator including cleanups
     * @param handle
     * @return
     */
    private static Data convert(DataHandle handle) {

        // Result
        List<String[]> dataset = new ArrayList<>();
        
        // Header
        String[] header = new String[] {
            FIELD_AGE,
            FIELD_GENDER,
            FIELD_DIAGNOSIS_MONTH,
            FIELD_DIAGNOSIS_YEAR,
            FIELD_PHASE_UNCOMPLICATED,
            FIELD_PHASE_COMPLICATED,
            FIELD_PHASE_CRITICAL,
            FIELD_PHASE_RECOVERY,
            FIELD_PHASE_COMPLICATED_VASSOPRESSORS,
            FIELD_PHASE_CRITICIAL_VASSOPRESSORS,
            FIELD_PHASE_CRITICIAL_VENTILATION,
            FIELD_PHASE_UNCOMPLICATED_SUPERINFECTION,
            FIELD_PHASE_COMPLICATED_SUPERINFECTION,
            FIELD_PHASE_CRITICIAL_SUPERINFECTION,
            FIELD_PHASE_RECOVERY_SYMPTOMS,
            FIELD_LAST_KNOWN_STATUS
        };
        
        // Add header
        dataset.add(header);

        // Convert rows
        for (int rowNumber = 0; rowNumber < handle.getNumRows(); rowNumber++) {

            // Initialize
            String[] row = header.clone();

            // For each column
            for (int colNumber = 0; colNumber < handle.getNumColumns(); colNumber++) {

                setValue(row, FIELD_AGE, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_AGE))));
                setValue(row, FIELD_GENDER, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_GENDER))));
                setValue(row, FIELD_DIAGNOSIS_MONTH, convert(convertDateToMonth(handle.getValue(rowNumber, handle.getColumnIndexOf(LEGACY_FIELD_DIAGNOSIS_MONTH_YEAR)))));
                setValue(row, FIELD_DIAGNOSIS_YEAR, convert(convertDateToYear(handle.getValue(rowNumber, handle.getColumnIndexOf(LEGACY_FIELD_DIAGNOSIS_MONTH_YEAR)))));
                setValue(row, FIELD_PHASE_UNCOMPLICATED, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_UNCOMPLICATED))));
                setValue(row, FIELD_PHASE_COMPLICATED, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_COMPLICATED))));
                setValue(row, FIELD_PHASE_CRITICAL, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_CRITICAL))));
                setValue(row, FIELD_PHASE_RECOVERY, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_RECOVERY))));
                setValue(row, FIELD_PHASE_COMPLICATED_VASSOPRESSORS, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_COMPLICATED_VASSOPRESSORS))));
                setValue(row, FIELD_PHASE_CRITICIAL_VASSOPRESSORS, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_CRITICIAL_VASSOPRESSORS))));
                setValue(row, FIELD_PHASE_CRITICIAL_VENTILATION, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_CRITICIAL_VENTILATION))));
                setValue(row, FIELD_PHASE_UNCOMPLICATED_SUPERINFECTION, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_UNCOMPLICATED_SUPERINFECTION))));
                setValue(row, FIELD_PHASE_COMPLICATED_SUPERINFECTION, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_COMPLICATED_SUPERINFECTION))));
                setValue(row, FIELD_PHASE_CRITICIAL_SUPERINFECTION, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_CRITICIAL_SUPERINFECTION))));
                setValue(row, FIELD_PHASE_RECOVERY_SYMPTOMS, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_PHASE_RECOVERY_SYMPTOMS))));
                setValue(row, FIELD_LAST_KNOWN_STATUS, convert(handle.getValue(rowNumber, handle.getColumnIndexOf(FIELD_LAST_KNOWN_STATUS))));
            }
            
            // Add
            dataset.add(row);
        }
        
        // Done
        return Data.create(dataset);
    }

    /**
     * Convert all values
     * @param value
     * @return
     */
    private static String convert(String value) {
        value = value.trim();
        if ("unknown".equals(value.toLowerCase())) value = VALUE_UNKNOWN_MISSING;
        if ("missing".equals(value.toLowerCase())) value = VALUE_UNKNOWN_MISSING;
        return value;
    }

    /**
     * Date to month
     * @param value
     * @return
     */
    private static String convertDateToMonth(String value) {
        if (value.equals(VALUE_NA) || !value.contains("_")) {
            return VALUE_NA;
        }
        return value.substring(0, value.indexOf("_"));
    }
    
    /**
     * Date to year
     * @param value
     * @return
     */
    private static String convertDateToYear(String value) {
        if (value.equals(VALUE_NA) || !value.contains("_")) {
            return VALUE_NA;
        }
        return value.substring(value.indexOf("_") + 1, value.length());
    }

    /**
     * Sets value to row
     * @param row
     * @param field
     * @param value
     */
    private static void setValue(String[] row, String field, String value) {

        // Insert values
        for (int i = 0; i < row.length; i++) {
            if (row[i].equals(field)) {
                row[i] = value;
            }
        }
    }
}
