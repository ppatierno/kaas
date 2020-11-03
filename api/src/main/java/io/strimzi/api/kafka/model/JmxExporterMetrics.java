/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.strimzi.crdgenerator.annotations.Description;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * JMX metrics config
 */
@Buildable(
        editableEnabled = false,
        builderPackage = Constants.FABRIC8_KUBERNETES_API
)
@JsonPropertyOrder({"valueFrom"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
public class JmxExporterMetrics implements Serializable, UnknownPropertyPreserving {

    private static final long serialVersionUID = 1L;

    private ExternalConfigurationMetrics valueFrom;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @Description("Value of the name and key if ConfigMap containing JMX configuration.")
    @JsonProperty(required = true)
    public ExternalConfigurationMetrics getValueFrom() {
        return valueFrom;
    }

    public void setValueFrom(ExternalConfigurationMetrics valueFrom) {
        this.valueFrom = valueFrom;
    }

    @Override
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @Override
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
