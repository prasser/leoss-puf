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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.AttributeType.Hierarchy.DefaultHierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.criteria.HierarchicalDistanceTCloseness;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.PrivacyCriterion;
import org.deidentifier.arx.criteria.SampleUniqueness;
import org.deidentifier.arx.exceptions.RollbackRequiredException;
import org.deidentifier.arx.metric.Metric;

/**
 * Implements all anonymization processes
 * @author Fabian Prasser
 */
public class Anon {
    
    /**
     * Privacy model
     * @author Fabian Prasser
     */
    private static class PrivacyModel {

        /** Model */
        public final PrivacyCriterion              model;
        /** Attributes */
        public final Pair<String, AttributeType>[] attributes;
        /**
         * Creates a new instance
         * @param model
         * @param type
         * @param attributes
         */
        @SafeVarargs
        private PrivacyModel(PrivacyCriterion model, Pair<String, AttributeType>... attributes) {
            this.model = model;
            this.attributes = attributes;
        }
    }

    /** Transformation rule */
    private static final Hierarchy RULE_AGE          = getAgeHierarchy();
    /** Transformation rule */
    private static final Hierarchy RULE_GENDER       = getGenderHierarchy();
    /** Transformation rule */
    private static final Hierarchy RULE_MONTH        = getMonthHierarchy();
    /** Transformation rule */
    private static final Hierarchy RULE_YEAR         = getYearHierarchy();
    /** Transformation rule */
    private static final Hierarchy RULE_STATUS       = getStatusHierarchy();
    /** Transformation rule */
    private static final Hierarchy RULE_INTERVENTION = getInterventionHierarchy();
    /** Transformation rule */
    private static final Hierarchy RULE_INFECTION    = getInfectionHierarchy();
    /** Transformation rule */
    private static final Hierarchy RULE_SYMPTOMS     = getSymptomsHierarchy();
    
    /**
     * Implements the anonymization process laid out in the ethics proposal
     * @param data
     * @throws IOException 
     * @throws RollbackRequiredException 
     */
    public static Data anonymizeFirstStage(Data data) throws IOException, RollbackRequiredException {
        
        // Report
        Report.registerInput("First stage", data.getHandle());

        // Variables
        String[] variables = new String[] {
                                           IO.FIELD_AGE,
                                           IO.FIELD_GENDER,
                                           IO.FIELD_DIAGNOSIS_MONTH,
                                           IO.FIELD_DIAGNOSIS_YEAR,
                                           IO.FIELD_PHASE_UNCOMPLICATED,
                                           IO.FIELD_PHASE_COMPLICATED,
                                           IO.FIELD_PHASE_CRITICAL,
                                           IO.FIELD_PHASE_RECOVERY,
                                           IO.FIELD_PHASE_COMPLICATED_VASSOPRESSORS,
                                           IO.FIELD_PHASE_CRITICIAL_VASSOPRESSORS,
                                           IO.FIELD_PHASE_CRITICIAL_VENTILATION,
                                           IO.FIELD_PHASE_UNCOMPLICATED_SUPERINFECTION,
                                           IO.FIELD_PHASE_COMPLICATED_SUPERINFECTION,
                                           IO.FIELD_PHASE_CRITICIAL_SUPERINFECTION,
                                           IO.FIELD_PHASE_RECOVERY_SYMPTOMS,
                                           IO.FIELD_LAST_KNOWN_STATUS                       
        };
        
        // Effects
        List<Pair<String, Integer>> effects = new ArrayList<>();
        
        // For each variable
        for (String variable : variables) {
            
            // Anonymize
            DataHandle handle = anonymize(data, effects, new PrivacyModel(new KAnonymity(10), new Pair<>(variable, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE)));
                        
            // Convert
            data = Util.getData(handle);
        }

        // Report
        Report.registerOutput("First stage", data.getHandle(), effects);
        
        // Done
        return data;
    }

    /**
     * Generalizes the quasi-identifiers
     * @param data
     * @return
     * @throws IOException 
     */
    public static Data anonymizeGeneralize(Data data) throws IOException {

        // Define all as insensitive
        for (int i = 0; i < data.getHandle().getNumColumns(); i++) {
            data.getDefinition().setAttributeType(data.getHandle().getAttributeName(i), AttributeType.INSENSITIVE_ATTRIBUTE);
        }

        // Define quasi-identifiers
        data.getDefinition().setAttributeType(IO.FIELD_AGE, RULE_AGE);
        data.getDefinition().setAttributeType(IO.FIELD_GENDER, RULE_GENDER);
        data.getDefinition().setAttributeType(IO.FIELD_DIAGNOSIS_MONTH, RULE_MONTH);
        data.getDefinition().setAttributeType(IO.FIELD_DIAGNOSIS_YEAR, RULE_YEAR);

        // Fix generalization levels
        data.getDefinition().setMinimumGeneralization(IO.FIELD_AGE, 1);
        data.getDefinition().setMaximumGeneralization(IO.FIELD_AGE, 1);
        data.getDefinition().setMinimumGeneralization(IO.FIELD_GENDER, 0);
        data.getDefinition().setMaximumGeneralization(IO.FIELD_GENDER, 0);
        data.getDefinition().setMinimumGeneralization(IO.FIELD_DIAGNOSIS_MONTH, 1);
        data.getDefinition().setMaximumGeneralization(IO.FIELD_DIAGNOSIS_MONTH, 1);
        data.getDefinition().setMinimumGeneralization(IO.FIELD_DIAGNOSIS_YEAR, 0);
        data.getDefinition().setMaximumGeneralization(IO.FIELD_DIAGNOSIS_YEAR, 0);

        // Prepare config
        ARXConfiguration config = ARXConfiguration.create();
        
        // Configure transformation model
        config.setSuppressionLimit(0d);
        config.addPrivacyModel(new SampleUniqueness(1d));
        config.setQualityModel(Metric.createLossMetric());
        
        // Anonymize
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        DataHandle handle = result.getOutput();
        
        // Sanity check
        if (handle.getStatistics().getEquivalenceClassStatistics().getNumberOfSuppressedRecords() != 0) {
            throw new IllegalStateException("Internal error! This must not happen.");
        }
        
        // Done
        return Util.getData(handle);
    }
    
    /**
     * Implements an additional quantitative anonymization process for maximal performance
     * @param data
     * @throws IOException 
     * @throws RollbackRequiredException 
     */
    public static Data anonymizeSecondStage(Data data) throws IOException, RollbackRequiredException {

        // Report
        Report.registerInput("Second stage", data.getHandle());
        
        // Privacy model
        PrivacyModel model1 = new PrivacyModel(new KAnonymity(11), 
                                               new Pair<>(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE));

        // Privacy model
        PrivacyModel model2 = new PrivacyModel(getPrivacyModel(IO.FIELD_LAST_KNOWN_STATUS, RULE_STATUS),
                                               new Pair<>(IO.FIELD_LAST_KNOWN_STATUS, AttributeType.SENSITIVE_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE));
        
        // Privacy model
        PrivacyModel model3 = new PrivacyModel(getPrivacyModel(IO.FIELD_PHASE_COMPLICATED_VASSOPRESSORS, RULE_INTERVENTION),
                                               new Pair<>(IO.FIELD_PHASE_COMPLICATED_VASSOPRESSORS, AttributeType.SENSITIVE_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE));
        
        // Privacy model
        PrivacyModel model4 = new PrivacyModel(getPrivacyModel(IO.FIELD_PHASE_CRITICIAL_VASSOPRESSORS, RULE_INTERVENTION),
                                               new Pair<>(IO.FIELD_PHASE_CRITICIAL_VASSOPRESSORS, AttributeType.SENSITIVE_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE));
        
        // Privacy model
        PrivacyModel model5 = new PrivacyModel(getPrivacyModel(IO.FIELD_PHASE_CRITICIAL_VENTILATION, RULE_INTERVENTION),
                                               new Pair<>(IO.FIELD_PHASE_CRITICIAL_VENTILATION, AttributeType.SENSITIVE_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE));
        
        // Privacy model
        PrivacyModel model6 = new PrivacyModel(getPrivacyModel(IO.FIELD_PHASE_UNCOMPLICATED_SUPERINFECTION, RULE_INFECTION),
                                               new Pair<>(IO.FIELD_PHASE_UNCOMPLICATED_SUPERINFECTION, AttributeType.SENSITIVE_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE));
        
        // Privacy model
        PrivacyModel model7 = new PrivacyModel(getPrivacyModel(IO.FIELD_PHASE_COMPLICATED_SUPERINFECTION, RULE_INFECTION),
                                               new Pair<>(IO.FIELD_PHASE_COMPLICATED_SUPERINFECTION, AttributeType.SENSITIVE_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE));
        
        // Privacy model
        PrivacyModel model8 = new PrivacyModel(getPrivacyModel(IO.FIELD_PHASE_CRITICIAL_SUPERINFECTION, RULE_INFECTION),
                                               new Pair<>(IO.FIELD_PHASE_CRITICIAL_SUPERINFECTION, AttributeType.SENSITIVE_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE));
        
        // Privacy model
        PrivacyModel model9 = new PrivacyModel(getPrivacyModel(IO.FIELD_PHASE_RECOVERY_SYMPTOMS, RULE_SYMPTOMS),
                                               new Pair<>(IO.FIELD_PHASE_RECOVERY_SYMPTOMS, AttributeType.SENSITIVE_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_AGE, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_GENDER, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_MONTH, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE),
                                               new Pair<>(IO.FIELD_DIAGNOSIS_YEAR, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE));

        // Effects
        List<Pair<String, Integer>> effects = new ArrayList<>();
        
        // Anonymize
        DataHandle handle = anonymize(data, effects, model1, model2, model3, model4, model5, model6, model7, model8, model9);

        // Report
        Report.registerOutput("Second stage", Util.getData(handle).getHandle(), effects);
        
        // Done
        return Util.getData(handle);
    }
    
    /**
     * Returns the privacy model for a specific sensitive attribute
     * @param attribute
     * @param transformationRule
     * @return
     */
    private static PrivacyCriterion getPrivacyModel(String attribute, Hierarchy transformationRule) {
        return new HierarchicalDistanceTCloseness(attribute, 0.5d, transformationRule);
    }

    /**
     * Internal anonymization method to be able to generate statistics
     * @param data
     * @param effects
     * @param models
     * @return
     * @throws IOException 
     */
    private static DataHandle anonymize(Data data, List<Pair<String, Integer>> effects, PrivacyModel... models) throws IOException {
        for (PrivacyModel model : models) {
            DataHandle handle = anonymize(Util.getData(data.getHandle()), model);
            effects.add(new Pair<>(model.attributes[0].getFirst() + ", " + model.model.toString(), handle.getStatistics().getEquivalenceClassStatistics().getNumberOfSuppressedRecords()));
        }
        return anonymize(data, models);
    }
    
    /**
     * Internal anonymization method to be able to generate statistics
     * @param data
     * @param effects
     * @param models
     * @return
     * @throws IOException 
     */
    private static DataHandle anonymize(Data data, PrivacyModel... models) throws IOException {

        // Define all as insensitive
        for (int i = 0; i < data.getHandle().getNumColumns(); i++) {
            data.getDefinition().setAttributeType(data.getHandle().getAttributeName(i), AttributeType.INSENSITIVE_ATTRIBUTE);
        }

        // Prepare config
        ARXConfiguration config = ARXConfiguration.create();
        config.setSuppressionLimit(1d);
        config.setQualityModel(Metric.createLossMetric());
        
        // Now add the privacy models
        for (PrivacyModel model : models) {
            for (Pair<String, AttributeType> attribute : model.attributes) {
                data.getDefinition().setAttributeType(attribute.getFirst(), attribute.getSecond());
            }
            config.addPrivacyModel(model.model);
        }
        
        // Anonymize
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        return anonymizer.anonymize(data, config).getOutput();
    }

    /**
     * Age hierarchy
     * @return
     */
    private static Hierarchy getAgeHierarchy() {
        DefaultHierarchy hierarchy = Hierarchy.create();
        hierarchy.add("< 1 years",          "<= 25 years");
        hierarchy.add("1 - 3 years",        "<= 25 years");
        hierarchy.add("4 - 8 years",        "<= 25 years");
        hierarchy.add("9 - 14 years",       "<= 25 years");
        hierarchy.add("15 - 25 year",       "<= 25 years");
        hierarchy.add("26 - 35 years",      "26 - 45 years");
        hierarchy.add("36 - 45 years",      "26 - 45 years");
        hierarchy.add("46 - 55 years",      "46 - 65 years");
        hierarchy.add("56 - 65 years",      "46 - 65 years");
        hierarchy.add("66 - 75 years",      "66 - 85 years");
        hierarchy.add("76 - 85 years",      "66 - 85 years");
        hierarchy.add("> 85 years",         "> 85 years");
        return hierarchy;
    }

    /**
     * Gender hierarchy
     * @return
     */
    private static Hierarchy getGenderHierarchy() {
        DefaultHierarchy hierarchy = Hierarchy.create();
        hierarchy.add("Female");
        hierarchy.add("Male");
        return hierarchy;
    }
    
    /**
     * Infection hierarchy
     * @return
     */
    private static Hierarchy getInfectionHierarchy() {
        DefaultHierarchy hierarchy = Hierarchy.create();
        hierarchy.add("bacterial", "bacterial and/or fungal", "*");
        hierarchy.add("fungal", "bacterial and/or fungal", "*");
        hierarchy.add("bacterial&fungal", "bacterial and/or fungal", "*");
        hierarchy.add("none", "none or unknown/missing or n/a", "*");
        hierarchy.add("n/a", "none or unknown/missing or n/a", "*");
        hierarchy.add("unknown/missing", "none or unknown/missing or n/a", "*");
        return hierarchy;
    }

    /**
     * Intervention hierarchy
     * @return
     */
    private static Hierarchy getInterventionHierarchy() {
        DefaultHierarchy hierarchy = Hierarchy.create();
        hierarchy.add("yes",                "yes or no",                "*");
        hierarchy.add("no",                 "yes or no",                "*");
        hierarchy.add("n/a",                "unknown/missing or n/a",   "*");
        hierarchy.add("unknown/missing",    "unknown/missing or n/a",   "*");
        return hierarchy;
    }

    /**
     * Month hierarchy
     * @return
     */
    private static Hierarchy getMonthHierarchy() {
        DefaultHierarchy hierarchy = Hierarchy.create();
        hierarchy.add("1", "<= 3");
        hierarchy.add("2", "<= 3");
        hierarchy.add("3", "<= 3");
        hierarchy.add("4", "4");
        hierarchy.add("5", "5");
        hierarchy.add("6", "6");
        hierarchy.add("7", "7");
        hierarchy.add("8", "8");
        hierarchy.add("9", "9");
        hierarchy.add("10", "10");
        hierarchy.add("11", "11");
        hierarchy.add("12", "12");
        return hierarchy;
    }
    
    /**
     * Status hierarchy
     * @return
     */
    private static Hierarchy getStatusHierarchy() {
        DefaultHierarchy hierarchy = Hierarchy.create();
        hierarchy.add("Dead from COVID-19",                                 "dead",     "*");
        hierarchy.add("Dead from other causes",                             "dead",     "*");
        hierarchy.add("Not recovered (means recovery phase not achieved)",  "not dead", "*");
        hierarchy.add("Recovered",                                          "not dead", "*");
        hierarchy.add("n/a",                                  "unknown/missing or n/a", "*");
        hierarchy.add("unknown/missing",                      "unknown/missing or n/a", "*");
        return hierarchy;
    }

    /**
     * Phase hierarchy
     * @return
     */
    private static Hierarchy getSymptomsHierarchy() {
        DefaultHierarchy hierarchy = Hierarchy.create();
        hierarchy.add("yes",                "yes or no",                "*");
        hierarchy.add("no",                 "yes or no",                "*");
        hierarchy.add("n/a",                "unknown/missing or n/a",   "*");
        hierarchy.add("unknown/missing",    "unknown/missing or n/a",   "*");
        return hierarchy;
    }

    /**
     * Year hierarchy
     * @return
     */
    private static Hierarchy getYearHierarchy() {
        DefaultHierarchy hierarchy = Hierarchy.create();
        hierarchy.add("2020");
        hierarchy.add("2021");
        return hierarchy;
    }
}