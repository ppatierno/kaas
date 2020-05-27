/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.operator.cluster.model;

import io.strimzi.api.kafka.model.KafkaMirrorMakerProducerSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for handling Kafka Mirror Maker producer configuration passed by the user
 */
public class KafkaMirrorMakerProducerConfiguration extends AbstractConfiguration {
    private static final List<String> FORBIDDEN_PREFIXES;
    private static final List<String> FORBIDDEN_PREFIX_EXCEPTIONS;
    private static final Map<String, String> DEFAULTS;

    static {
        FORBIDDEN_PREFIXES = AbstractConfiguration.splitPrefixesToList(KafkaMirrorMakerProducerSpec.FORBIDDEN_PREFIXES);
        FORBIDDEN_PREFIX_EXCEPTIONS = AbstractConfiguration.splitPrefixesToList(KafkaMirrorMakerProducerSpec.FORBIDDEN_PREFIX_EXCEPTIONS);
        DEFAULTS = new HashMap<>(0);
    }

    /**
     * Constructor used to instantiate this class from JsonObject. Should be used to create configuration from
     * ConfigMap / CRD.
     *
     * @param jsonOptions     Json object with configuration options as key ad value pairs.
     */
    public KafkaMirrorMakerProducerConfiguration(Iterable<Map.Entry<String, Object>> jsonOptions) {
        super(jsonOptions, FORBIDDEN_PREFIXES, FORBIDDEN_PREFIX_EXCEPTIONS, DEFAULTS);
    }
}
