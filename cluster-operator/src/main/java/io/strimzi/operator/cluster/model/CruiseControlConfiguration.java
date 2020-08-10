/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.operator.cluster.model;

import io.strimzi.api.kafka.model.CruiseControlSpec;
import io.strimzi.operator.cluster.operator.resource.cruisecontrol.CruiseControlConfigurationGoals;
import io.strimzi.operator.cluster.operator.resource.cruisecontrol.CruiseControlConfigurationParameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for handling Cruise Control configuration passed by the user
 */
public class CruiseControlConfiguration extends AbstractConfiguration {

    /**
     * A list of case insensitive goals that Cruise Control supports in the order of priority.
     * The high priority goals will be executed first.
     */
    protected static final List<String> CRUISE_CONTROL_GOALS_LIST = Collections.unmodifiableList(
        Arrays.asList(
                CruiseControlConfigurationGoals.RACK_AWARENESS_GOAL.toString(),
                CruiseControlConfigurationGoals.REPLICA_CAPACITY_GOAL.toString(),
                CruiseControlConfigurationGoals.DISK_CAPACITY_GOAL.toString(),
                CruiseControlConfigurationGoals.NETWORK_INBOUND_CAPACITY_GOAL.toString(),
                CruiseControlConfigurationGoals.NETWORK_OUTBOUND_CAPACITY_GOAL.toString(),
                CruiseControlConfigurationGoals.CPU_CAPACITY_GOAL.toString(),
                CruiseControlConfigurationGoals.REPLICA_DISTRIBUTION_GOAL.toString(),
                CruiseControlConfigurationGoals.POTENTIAL_NETWORK_OUTAGE_GOAL.toString(),
                CruiseControlConfigurationGoals.DISK_USAGE_DISTRIBUTION_GOAL.toString(),
                CruiseControlConfigurationGoals.NETWORK_INBOUND_USAGE_DISTRIBUTION_GOAL.toString(),
                CruiseControlConfigurationGoals.NETWORK_OUTBOUND_USAGE_DISTRIBUTION_GOAL.toString(),
                CruiseControlConfigurationGoals.CPU_USAGE_DISTRIBUTION_GOAL.toString(),
                CruiseControlConfigurationGoals.TOPIC_REPLICA_DISTRIBUTION_GOAL.toString(),
                CruiseControlConfigurationGoals.LEADER_REPLICA_DISTRIBUTION_GOAL.toString(),
                CruiseControlConfigurationGoals.LEADER_BYTES_IN_DISTRIBUTION_GOAL.toString(),
                CruiseControlConfigurationGoals.PREFERRED_LEADER_ELECTION_GOAL.toString()
        )
     );

    public static final String CRUISE_CONTROL_GOALS = String.join(",", CRUISE_CONTROL_GOALS_LIST);

    /**
     * A list of case insensitive goals that Cruise Control will use as hard goals that must all be met for an optimization
     * proposal to be valid.
     */
    protected static final List<String> CRUISE_CONTROL_HARD_GOALS_LIST = Collections.unmodifiableList(
            Arrays.asList(
                    CruiseControlConfigurationGoals.RACK_AWARENESS_GOAL.toString(),
                    CruiseControlConfigurationGoals.REPLICA_CAPACITY_GOAL.toString(),
                    CruiseControlConfigurationGoals.DISK_CAPACITY_GOAL.toString(),
                    CruiseControlConfigurationGoals.NETWORK_INBOUND_CAPACITY_GOAL.toString(),
                    CruiseControlConfigurationGoals.NETWORK_OUTBOUND_CAPACITY_GOAL.toString(),
                    CruiseControlConfigurationGoals.CPU_CAPACITY_GOAL.toString()
            )
    );

    public static final String CRUISE_CONTROL_HARD_GOALS = String.join(",", CRUISE_CONTROL_HARD_GOALS_LIST);

    protected static final List<String> CRUISE_CONTROL_DEFAULT_ANOMALY_DETECTION_GOALS_LIST = Collections.unmodifiableList(
        Arrays.asList(
                CruiseControlConfigurationGoals.RACK_AWARENESS_GOAL.toString(),
                CruiseControlConfigurationGoals.REPLICA_CAPACITY_GOAL.toString(),
                CruiseControlConfigurationGoals.DISK_CAPACITY_GOAL.toString()
        )
    );

    public static final String CRUISE_CONTROL_DEFAULT_ANOMALY_DETECTION_GOALS =
            String.join(",", CRUISE_CONTROL_DEFAULT_ANOMALY_DETECTION_GOALS_LIST);

    public static final String CRUISE_CONTROL_GOALS_CONFIG_KEY = "goals";
    public static final String CRUISE_CONTROL_DEFAULT_GOALS_CONFIG_KEY = "default.goals";
    public static final String CRUISE_CONTROL_HARD_GOALS_CONFIG_KEY = "hard.goals";
    public static final String CRUISE_CONTROL_SELF_HEALING_CONFIG_KEY = "self.healing.goals";
    public static final String CRUISE_CONTROL_ANOMALY_DETECTION_CONFIG_KEY = "anomaly.detection.goals";
   /*
    * Map containing default values for required configuration properties
    */
    private static final Map<String, String> CRUISE_CONTROL_DEFAULT_PROPERTIES_MAP;

    private static final List<String> FORBIDDEN_PREFIXES;
    private static final List<String> FORBIDDEN_PREFIX_EXCEPTIONS;

    static {
        Map<String, String> config = new HashMap<>(8);
        config.put(CruiseControlConfigurationParameters.CRUISE_CONTROL_PARTITION_METRICS_WINDOW_MS_CONFIG_KEY.getName(),
                CruiseControlConfigurationParameters.CRUISE_CONTROL_PARTITION_METRICS_WINDOW_MS_CONFIG_KEY.getDefaultValue().toString());
        config.put(CruiseControlConfigurationParameters.CRUISE_CONTROL_PARTITION_METRICS_WINDOW_NUM_CONFIG_KEY.getName(),
                CruiseControlConfigurationParameters.CRUISE_CONTROL_PARTITION_METRICS_WINDOW_NUM_CONFIG_KEY.getDefaultValue().toString());
        config.put(CruiseControlConfigurationParameters.CRUISE_CONTROL_BROKER_METRICS_WINDOW_MS_CONFIG_KEY.getName(),
                CruiseControlConfigurationParameters.CRUISE_CONTROL_BROKER_METRICS_WINDOW_MS_CONFIG_KEY.getDefaultValue().toString());
        config.put(CruiseControlConfigurationParameters.CRUISE_CONTROL_BROKER_METRICS_WINDOW_NUM_CONFIG_KEY.getName(),
                CruiseControlConfigurationParameters.CRUISE_CONTROL_BROKER_METRICS_WINDOW_NUM_CONFIG_KEY.getDefaultValue().toString());
        config.put(CruiseControlConfigurationParameters.CRUISE_CONTROL_COMPLETED_USER_TASK_RETENTION_MS_CONFIG_KEY.getName(),
                CruiseControlConfigurationParameters.CRUISE_CONTROL_COMPLETED_USER_TASK_RETENTION_MS_CONFIG_KEY.getDefaultValue().toString());
        config.put(CRUISE_CONTROL_GOALS_CONFIG_KEY, CRUISE_CONTROL_GOALS);
        config.put(CRUISE_CONTROL_DEFAULT_GOALS_CONFIG_KEY, CRUISE_CONTROL_GOALS);
        config.put(CRUISE_CONTROL_HARD_GOALS_CONFIG_KEY, CRUISE_CONTROL_HARD_GOALS);
        CRUISE_CONTROL_DEFAULT_PROPERTIES_MAP = Collections.unmodifiableMap(config);

        FORBIDDEN_PREFIXES = AbstractConfiguration.splitPrefixesToList(CruiseControlSpec.FORBIDDEN_PREFIXES);
        FORBIDDEN_PREFIX_EXCEPTIONS = AbstractConfiguration.splitPrefixesToList(CruiseControlSpec.FORBIDDEN_PREFIX_EXCEPTIONS);
    }

    /**
     * Constructor used to instantiate this class from JsonObject. Should be used to create configuration from
     * ConfigMap / CRD.
     *
     * @param jsonOptions     Json object with configuration options as key ad value pairs.
     */
    public CruiseControlConfiguration(Iterable<Map.Entry<String, Object>> jsonOptions) {
        super(jsonOptions, FORBIDDEN_PREFIXES, FORBIDDEN_PREFIX_EXCEPTIONS);
    }

    private CruiseControlConfiguration(String configuration, List<String> forbiddenPrefixes) {
        super(configuration, forbiddenPrefixes);
    }

    public static Map<String, String> getCruiseControlDefaultPropertiesMap() {
        return CRUISE_CONTROL_DEFAULT_PROPERTIES_MAP;
    }
}
