/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.operator.cluster.model;

import io.strimzi.api.kafka.model.KafkaMirrorMaker2ClusterSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for handling Kafka MirrorMaker 2.0 connect configuration passed by the user
 */
public class KafkaMirrorMaker2Configuration extends AbstractConfiguration {
    private static final List<String> FORBIDDEN_PREFIXES;
    private static final List<String> FORBIDDEN_PREFIX_EXCEPTIONS;
    private static final Map<String, String> DEFAULTS;

    static {
        FORBIDDEN_PREFIXES = AbstractConfiguration.splitPrefixesToList(KafkaMirrorMaker2ClusterSpec.FORBIDDEN_PREFIXES);
        FORBIDDEN_PREFIX_EXCEPTIONS = AbstractConfiguration.splitPrefixesToList(KafkaMirrorMaker2ClusterSpec.FORBIDDEN_PREFIX_EXCEPTIONS);

        DEFAULTS = new HashMap<>(8);
        DEFAULTS.put("group.id", "mirrormaker2-cluster");
        DEFAULTS.put("offset.storage.topic", "mirrormaker2-cluster-offsets");
        DEFAULTS.put("config.storage.topic", "mirrormaker2-cluster-configs");
        DEFAULTS.put("status.storage.topic", "mirrormaker2-cluster-status");
        DEFAULTS.put("key.converter", "org.apache.kafka.connect.converters.ByteArrayConverter");
        DEFAULTS.put("value.converter", "org.apache.kafka.connect.converters.ByteArrayConverter");
        DEFAULTS.put("config.providers", "file");
        DEFAULTS.put("config.providers.file.class", "org.apache.kafka.common.config.provider.FileConfigProvider");
    }

    /**
     * Constructor used to instantiate this class from JsonObject. Should be used to
     * create configuration from ConfigMap / CRD.
     *
     * @param jsonOptions Json object with configuration options as key ad value
     *                    pairs.
     */
    public KafkaMirrorMaker2Configuration(Iterable<Map.Entry<String, Object>> jsonOptions) {
        super(jsonOptions, FORBIDDEN_PREFIXES, FORBIDDEN_PREFIX_EXCEPTIONS, DEFAULTS);
    }
}
