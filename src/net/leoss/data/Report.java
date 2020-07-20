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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.math3.util.Pair;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.aggregates.StatisticsFrequencyDistribution;

/**
 * Anonymization report
 * @author Fabian Prasser
 */
public class Report {
    
    /** List of reports*/
    private static final List<Report> REPORTS = new ArrayList<>();

    /**
     * Returns the reports
     * @return
     */
    public static List<Report> getReports() {
        return Report.REPORTS;
    }
    /**
     * Register input
     * @param phase
     * @param handle
     */
    public static Report registerInput(String phase, DataHandle handle) {
        return register(phase, handle, true);
    }
    /**
     * Register output
     * @param phase
     * @param handle
     * @param effects 
     */
    public static Report registerOutput(String phase, DataHandle handle, List<Pair<String, Integer>> effects) {
        Report report = register(phase, handle, false);
        for (Pair<String, Integer> effect : effects) {
            report.addEffect(effect.getFirst(), effect.getSecond());
        }
        return report;
    }

    /**
     * Register data
     * @param phase
     * @param handle
     * @param input
     */
    private static Report register(String phase, DataHandle handle, boolean input) {
        LinkedHashMap<String, StatisticsFrequencyDistribution> map = new LinkedHashMap<>();
        for (int column = 0; column < handle.getNumColumns(); column++) {
            map.put(handle.getAttributeName(column), handle.getStatistics().getFrequencyDistribution(column));
        }
        Data data = Util.getData(handle);
        Report report = new Report(phase, input, map, data.getHandle().getNumRows(), Stats.getLowestRisk(data), Stats.getAverageRisk(data), Stats.getHighestRisk(data));
        REPORTS.add(report);
        return report;
    }

    /** Report properties */
    private final String                                                 phase;
    /** Report properties */
    private final boolean                                                input;
    /** Report properties */
    private final LinkedHashMap<String, StatisticsFrequencyDistribution> distributions;
    /** Effects */
    private final List<Pair<String, Integer>>                            effects = new ArrayList<>();
    /** Records */
    private final int                                                    records;
    /** Risk */
    private final double                                                 riskLowest;
    /** Risk */
    private final double                                                 riskHighest;
    /** Risk */
    private final double                                                 riskAverage;
    
    /**
     * Creates a new instance
     * @param phase
     * @param input
     * @param distributions
     * @param records
     * @param riskLowest
     * @param riskAverage
     * @param riskHighest
     */
    public Report(String phase, boolean input, LinkedHashMap<String, StatisticsFrequencyDistribution> distributions, int records, double riskLowest, double riskAverage, double riskHighest) {
        this.phase = phase;
        this.input = input;
        this.distributions = distributions;
        this.records = records;
        this.riskLowest = riskLowest;
        this.riskHighest = riskHighest;
        this.riskAverage = riskAverage;
    }
    
    /**
     * Adds an effect
     * @param model
     * @param removed
     */
    public void addEffect(String model, int removed) {
        effects.add(new Pair<>(model, removed));
    }
    
    /**
     * Converts to string
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Report\n");
        builder.append("------\n");
        builder.append("- Phase: ").append(phase).append(" (").append(input ? "input" : "output").append(")\n");
        for (Entry<String, StatisticsFrequencyDistribution> entry : distributions.entrySet()) {
            builder.append("- Distribution: ").append(entry.getKey()).append(" [");
            String[] values = entry.getValue().values;
            double[] frequency = entry.getValue().frequency;
            for (int i=0; i< values.length; i++) {
                builder.append(values[i]).append(", ");
                builder.append(frequency[i]);
                if (i < values.length - 1) {
                    builder.append(", ");
                } else {
                    builder.append("]\n");
                }
            }
        }
        for (Pair<String, Integer> effect : effects) {
            builder.append("- Effect: ").append(effect.getFirst()).append(", records removed: ").append(effect.getSecond()).append("\n");
        }
        builder.append("- Total records to be released: ").append(records).append("\n");
        builder.append("- Highest re-identification risk: ").append(riskHighest).append("\n");
        builder.append("- Lowest re-identification risk: ").append(riskLowest).append("\n");
        builder.append("- Average re-identification risk: ").append(riskAverage).append("\n");
        return builder.toString();
    }
}
