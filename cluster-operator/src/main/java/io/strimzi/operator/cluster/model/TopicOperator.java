/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;


import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStrategy;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStrategyBuilder;
import io.strimzi.api.kafka.model.KafkaAssembly;
import io.strimzi.operator.cluster.operator.resource.RoleBindingOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the topic operator deployment
 */
public class TopicOperator extends AbstractModel {

    /**
     * The default kind of CMs that the Topic Operator will be configured to watch for
     */
    public static final String TOPIC_CM_KIND = "topic";

    private static final String NAME_SUFFIX = "-topic-operator";

    protected static final String METRICS_AND_LOG_CONFIG_SUFFIX = NAME_SUFFIX + "-config";

    // Port configuration
    protected static final int HEALTHCHECK_PORT = 8080;
    protected static final String HEALTHCHECK_PORT_NAME = "healthcheck";

    // Configuration defaults


    // Topic Operator configuration keys
    public static final String ENV_VAR_CONFIGMAP_LABELS = "STRIMZI_CONFIGMAP_LABELS";
    public static final String ENV_VAR_KAFKA_BOOTSTRAP_SERVERS = "STRIMZI_KAFKA_BOOTSTRAP_SERVERS";
    public static final String ENV_VAR_ZOOKEEPER_CONNECT = "STRIMZI_ZOOKEEPER_CONNECT";
    public static final String ENV_VAR_WATCHED_NAMESPACE = "STRIMZI_NAMESPACE";
    public static final String ENV_VAR_FULL_RECONCILIATION_INTERVAL_MS = "STRIMZI_FULL_RECONCILIATION_INTERVAL_MS";
    public static final String ENV_VAR_ZOOKEEPER_SESSION_TIMEOUT_MS = "STRIMZI_ZOOKEEPER_SESSION_TIMEOUT_MS";
    public static final String ENV_VAR_TOPIC_METADATA_MAX_ATTEMPTS = "STRIMZI_TOPIC_METADATA_MAX_ATTEMPTS";
    public static final String TO_CLUSTER_ROLE_NAME = "strimzi-topic-operator";
    public static final String TO_ROLE_BINDING_NAME = "strimzi-topic-operator-role-binding";

    // Kafka bootstrap servers and Zookeeper nodes can't be specified in the JSON
    private String kafkaBootstrapServers;
    private String zookeeperConnect;

    private String watchedNamespace;
    private int reconciliationIntervalMs;
    private int zookeeperSessionTimeoutMs;
    private String topicConfigMapLabels;
    private int topicMetadataMaxAttempts;

    /**
     * @param namespace Kubernetes/OpenShift namespace where cluster resources are going to be created
     * @param cluster   overall cluster name
     */
    protected TopicOperator(String namespace, String cluster, Labels labels) {

        super(namespace, cluster, labels);
        this.name = topicOperatorName(cluster);
        this.image = io.strimzi.api.kafka.model.TopicOperator.DEFAULT_IMAGE;
        this.replicas = io.strimzi.api.kafka.model.TopicOperator.DEFAULT_REPLICAS;
        this.readinessPath = "/";
        this.readinessTimeout = io.strimzi.api.kafka.model.TopicOperator.DEFAULT_HEALTHCHECK_TIMEOUT;
        this.readinessInitialDelay = io.strimzi.api.kafka.model.TopicOperator.DEFAULT_HEALTHCHECK_DELAY;
        this.livenessPath = "/";
        this.livenessTimeout = io.strimzi.api.kafka.model.TopicOperator.DEFAULT_HEALTHCHECK_TIMEOUT;
        this.livenessInitialDelay = io.strimzi.api.kafka.model.TopicOperator.DEFAULT_HEALTHCHECK_DELAY;

        // create a default configuration
        this.kafkaBootstrapServers = defaultBootstrapServers(cluster);
        this.zookeeperConnect = defaultZookeeperConnect(cluster);
        this.watchedNamespace = namespace;
        this.reconciliationIntervalMs = io.strimzi.api.kafka.model.TopicOperator.DEFAULT_FULL_RECONCILIATION_INTERVAL_SECONDS;
        this.zookeeperSessionTimeoutMs = io.strimzi.api.kafka.model.TopicOperator.DEFAULT_ZOOKEEPER_SESSION_TIMEOUT_SECONDS;
        this.topicConfigMapLabels = defaultTopicConfigMapLabels(cluster);
        this.topicMetadataMaxAttempts = io.strimzi.api.kafka.model.TopicOperator.DEFAULT_TOPIC_METADATA_MAX_ATTEMPTS;

        this.ancillaryConfigName = metricAndLogConfigsName(cluster);
        this.logAndMetricsConfigVolumeName = "topic-operator-metrics-and-logging";
        this.logAndMetricsConfigMountPath = "/opt/topic-operator/custom-config/";
        this.validLoggerFields = getDefaultLogConfig();
    }


    public void setWatchedNamespace(String watchedNamespace) {
        this.watchedNamespace = watchedNamespace;
    }

    public String getWatchedNamespace() {
        return watchedNamespace;
    }

    public void setTopicConfigMapLabels(String topicConfigMapLabels) {
        this.topicConfigMapLabels = topicConfigMapLabels;
    }

    public String getTopicConfigMapLabels() {
        return topicConfigMapLabels;
    }

    public void setReconciliationIntervalMs(int reconciliationIntervalMs) {
        this.reconciliationIntervalMs = reconciliationIntervalMs;
    }

    public int getReconciliationIntervalMs() {
        return reconciliationIntervalMs;
    }

    public void setZookeeperSessionTimeoutMs(int zookeeperSessionTimeoutMs) {
        this.zookeeperSessionTimeoutMs = zookeeperSessionTimeoutMs;
    }

    public int getZookeeperSessionTimeoutMs() {
        return zookeeperSessionTimeoutMs;
    }

    public void setKafkaBootstrapServers(String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public void setZookeeperConnect(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

    public String getZookeeperConnect() {
        return zookeeperConnect;
    }

    public void setTopicMetadataMaxAttempts(int topicMetadataMaxAttempts) {
        this.topicMetadataMaxAttempts = topicMetadataMaxAttempts;
    }

    public int getTopicMetadataMaxAttempts() {
        return topicMetadataMaxAttempts;
    }

    public static String topicOperatorName(String cluster) {
        return cluster + io.strimzi.operator.cluster.model.TopicOperator.NAME_SUFFIX;
    }

    public static String metricAndLogConfigsName(String cluster) {
        return cluster + TopicOperator.METRICS_AND_LOG_CONFIG_SUFFIX;
    }

    protected static String defaultZookeeperConnect(String cluster) {
        return ZookeeperCluster.serviceName(cluster) + ":" + io.strimzi.api.kafka.model.TopicOperator.DEFAULT_ZOOKEEPER_PORT;
    }

    protected static String defaultBootstrapServers(String cluster) {
        return KafkaCluster.serviceName(cluster) + ":" + io.strimzi.api.kafka.model.TopicOperator.DEFAULT_BOOTSTRAP_SERVERS_PORT;
    }

    protected static String defaultTopicConfigMapLabels(String cluster) {
        return String.format("%s=%s,%s=%s",
                Labels.STRIMZI_CLUSTER_LABEL, cluster,
                Labels.STRIMZI_KIND_LABEL, io.strimzi.operator.cluster.model.TopicOperator.TOPIC_CM_KIND);
    }

    /**
     * Create a Topic Operator from given desired resource
     *
     * @param resource desired resource with cluster configuration containing the topic operator one
     * @return Topic Operator instance, null if not configured in the ConfigMap
     */
    public static TopicOperator fromCrd(KafkaAssembly resource) {
        TopicOperator result;
        if (resource.getSpec().getTopicOperator() != null) {
            String namespace = resource.getMetadata().getNamespace();
            result = new TopicOperator(
                    namespace,
                    resource.getMetadata().getName(),
                    Labels.fromResource(resource).withKind(resource.getKind()));
            io.strimzi.api.kafka.model.TopicOperator tcConfig = resource.getSpec().getTopicOperator();
            result.setImage(tcConfig.getImage());
            result.setWatchedNamespace(tcConfig.getWatchedNamespace() != null ? tcConfig.getWatchedNamespace() : namespace);
            result.setReconciliationIntervalMs(tcConfig.getReconciliationIntervalSeconds() * 1_000);
            result.setZookeeperSessionTimeoutMs(tcConfig.getZookeeperSessionTimeoutSeconds() * 1_000);
            result.setTopicMetadataMaxAttempts(tcConfig.getTopicMetadataMaxAttempts());
            result.setLogging(tcConfig.getLogging());
            result.setResources(tcConfig.getResources());
            result.setUserAffinity(tcConfig.getAffinity());
        } else {
            result = null;
        }
        return result;
    }

    public Deployment generateDeployment() {
        DeploymentStrategy updateStrategy = new DeploymentStrategyBuilder()
                .withType("Recreate")
                .build();

        return createDeployment(
                updateStrategy,
                Collections.emptyMap(),
                Collections.emptyMap(),
                getMergedAffinity(),
                getInitContainers(),
                getContainers(),
                getVolumes()
        );
    }

    @Override
    protected List<Container> getContainers() {
        List<Container> containers = new ArrayList<>();
        Container container = new ContainerBuilder()
                .withName(name)
                .withImage(getImage())
                .withEnv(getEnvVars())
                .withPorts(Collections.singletonList(createContainerPort(HEALTHCHECK_PORT_NAME, HEALTHCHECK_PORT, "TCP")))
                .withLivenessProbe(createHttpProbe(livenessPath + "healthy", HEALTHCHECK_PORT_NAME, livenessInitialDelay, livenessTimeout))
                .withReadinessProbe(createHttpProbe(readinessPath + "ready", HEALTHCHECK_PORT_NAME, readinessInitialDelay, readinessTimeout))
                .withVolumeMounts(getVolumeMounts())
                .withResources(resources(getResources()))
                .build();

        containers.add(container);

        return containers;
    }

    @Override
    protected List<EnvVar> getEnvVars() {
        List<EnvVar> varList = new ArrayList<>();
        varList.add(buildEnvVar(ENV_VAR_CONFIGMAP_LABELS, topicConfigMapLabels));
        varList.add(buildEnvVar(ENV_VAR_KAFKA_BOOTSTRAP_SERVERS, kafkaBootstrapServers));
        varList.add(buildEnvVar(ENV_VAR_ZOOKEEPER_CONNECT, zookeeperConnect));
        varList.add(buildEnvVar(ENV_VAR_WATCHED_NAMESPACE, watchedNamespace));
        varList.add(buildEnvVar(ENV_VAR_FULL_RECONCILIATION_INTERVAL_MS, Integer.toString(reconciliationIntervalMs)));
        varList.add(buildEnvVar(ENV_VAR_ZOOKEEPER_SESSION_TIMEOUT_MS, Integer.toString(zookeeperSessionTimeoutMs)));
        varList.add(buildEnvVar(ENV_VAR_TOPIC_METADATA_MAX_ATTEMPTS, String.valueOf(topicMetadataMaxAttempts)));

        return varList;
    }

    /**
     * Get the name of the topic operator service account given the name of the {@code cluster}.
     */
    public static String topicOperatorServiceAccountName(String cluster) {
        return cluster + NAME_SUFFIX;
    }

    @Override
    protected String getServiceAccountName() {
        return topicOperatorServiceAccountName(cluster);
    }

    public ServiceAccount generateServiceAccount() {
        return new ServiceAccountBuilder()
                .withNewMetadata()
                    .withName(getServiceAccountName())
                    .withNamespace(namespace)
                .endMetadata()
            .build();
    }

    public RoleBindingOperator.RoleBinding generateRoleBinding(String namespace) {
        return new RoleBindingOperator.RoleBinding(TO_ROLE_BINDING_NAME, TO_CLUSTER_ROLE_NAME, namespace, getServiceAccountName());
    }

    @Override
    protected String getDefaultLogConfigFileName() {
        return "topicOperatorDefaultLoggingProperties";
    }

    @Override
    String getAncillaryConfigMapKeyLogConfig() {
        return "log4j2.properties";
    }

    private List<Volume> getVolumes() {
        return Collections.singletonList(createConfigMapVolume(logAndMetricsConfigVolumeName, ancillaryConfigName));
    }

    private List<VolumeMount> getVolumeMounts() {
        return Collections.singletonList(createVolumeMount(logAndMetricsConfigVolumeName, logAndMetricsConfigMountPath));
    }
}
