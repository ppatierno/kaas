/*
 * Copyright 2019, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.strimzi.crdgenerator.annotations.Description;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;

/**
 * Represents a status of the Kafka Connect resource
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "conditions", "observedGeneration", "restApiAddress" })
@EqualsAndHashCode
public class KafkaConnectStatus extends Status {
    private static final long serialVersionUID = 1L;

    private String restApiAddress;

    @Description("REST API endpoint for managing and monitoring Kafka Connect connectors.")
    public String getRestApiAddress() {
        return restApiAddress;
    }

    public void setRestApiAddress(String restApiAddress) {
        this.restApiAddress = restApiAddress;
    }
}