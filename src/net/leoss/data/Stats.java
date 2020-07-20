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

import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.Data;

/**
 * Statistics
 * @author Fabian Prasser
 */
public class Stats {

    /**
     * Risks
     * @param data
     * @return
     */
    public static double getAverageRisk(Data data) {

        // Define quasi-identifiers
        data.getDefinition().setAttributeType(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        data.getDefinition().setAttributeType(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        data.getDefinition().setAttributeType(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        data.getDefinition().setAttributeType(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        
        // Analyze
        return data.getHandle().getRiskEstimator().getSampleBasedReidentificationRisk().getAverageRisk();
    }

    /**
     * Risks
     * @param data
     * @return
     */
    public static double getHighestRisk(Data data) {

        // Define quasi-identifiers
        data.getDefinition().setAttributeType(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        data.getDefinition().setAttributeType(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        data.getDefinition().setAttributeType(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        data.getDefinition().setAttributeType(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);

        // Analyze
        return data.getHandle().getRiskEstimator().getSampleBasedReidentificationRisk().getHighestRisk();
    }

    /**
     * Risks
     * @param data
     * @return
     */
    public static double getLowestRisk(Data data) {

        // Define quasi-identifiers
        data.getDefinition().setAttributeType(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        data.getDefinition().setAttributeType(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        data.getDefinition().setAttributeType(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        data.getDefinition().setAttributeType(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);

        // Analyze
        return data.getHandle().getRiskEstimator().getSampleBasedReidentificationRisk().getLowestRisk();
    }
}
