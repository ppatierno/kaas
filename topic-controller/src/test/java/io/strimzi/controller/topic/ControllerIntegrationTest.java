/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.strimzi.controller.topic;

import io.debezium.kafka.KafkaCluster;
import io.debezium.kafka.ZookeeperServer;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(VertxUnitRunner.class)
public class ControllerIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ControllerIntegrationTest.class);

    private static final Oc OC = new Oc();
    private static Thread ocHook = new Thread(() -> {
        try {
            OC.clusterDown();
        } catch (Exception e) {}
    });
    private static boolean startedOc = false;

    private final LabelPredicate cmPredicate = LabelPredicate.fromString(
            "strimzi.io/kind=topic");

    private final Vertx vertx = Vertx.vertx();
    private KafkaCluster kafkaCluster;
    private volatile Controller controller;
    private volatile ControllerAssignedKafkaImpl kafka;
    private volatile AdminClient adminClient;
    private volatile K8sImpl k8s;
    private DefaultKubernetesClient kubeClient;
    private volatile TopicsWatcher topicsWatcher;
    private Thread kafkaHook = new Thread() {
        public void run() {
            if (kafkaCluster != null) {
                kafkaCluster.shutdown();
            }
        }
    };
    private final long timeout = 60_000L;
    private volatile TopicConfigsWatcher topicsConfigWatcher;

    private Session session;
    private volatile String deploymentId;
    private Set<String> preExistingEvents;

    @BeforeClass
    public static void startKube() throws Exception {
        logger.info("Executing oc cluster up");
        if (OC.isClusterUp()) {
            throw new RuntimeException("OpenShift cluster is already up");
        }
        // It can happen that if the VM exits abnormally the cluster remains up, and further tests don't work because
        // it appears there are two brokers with id 1, so use a shutdown hook to kill the cluster.
        startedOc = true;
        Runtime.getRuntime().addShutdownHook(ocHook);
        OC.clusterUp();
        OC.loginSystemAdmin();
        //OC.apply(new File("src/test/resources/role.yaml"));
        OC.loginDeveloper();
    }

    @AfterClass
    public static void stopKube() throws Exception {
        if (startedOc) {
            startedOc = false;
            logger.info("Executing oc cluster down");
            OC.clusterDown();
            Runtime.getRuntime().removeShutdownHook(ocHook);
        }
    }

    @Before
    public void setup(TestContext context) throws Exception {
        logger.info("Setting up test");
        Runtime.getRuntime().addShutdownHook(kafkaHook);
        kafkaCluster = new KafkaCluster();
        kafkaCluster.addBrokers(1);
        kafkaCluster.deleteDataPriorToStartup(true);
        kafkaCluster.deleteDataUponShutdown(true);
        kafkaCluster.usingDirectory(Files.createTempDirectory("controller-integration-test").toFile());
        kafkaCluster.startup();

        kubeClient = new DefaultKubernetesClient(OC.masterUrl());
        Map<String, String> m = new HashMap();
        m.put(Config.KAFKA_BOOTSTRAP_SERVERS.key, kafkaCluster.brokerList());
        m.put(Config.ZOOKEEPER_CONNECT.key, "localhost:"+ zkPort(kafkaCluster));
        Session session = new Session(kubeClient, new Config(m));

        Async async = context.async();
        vertx.deployVerticle(session, ar -> {
            if (ar.succeeded()) {
                deploymentId = ar.result();
                adminClient = session.adminClient;
                kafka = session.kafka;
                k8s = session.k8s;
                controller = session.controller;
                topicsConfigWatcher = session.tcw;
                topicsWatcher = session.tw;
                async.complete();
            } else {
                context.fail("Failed to deploy session");
            }
        });
        async.await();

        waitFor(context, () -> this.topicsWatcher.started(), timeout, "Topic watcher not started");
        waitFor(context, () -> this.topicsConfigWatcher.started(), timeout, "Topic configs watcher not started");

        // We can't delete events, so record the events which exist at the start of the test
        // and then waitForEvents() can ignore those
        preExistingEvents = kubeClient.events().withLabels(cmPredicate.labels()).list().
                getItems().stream().
                map(evt -> evt.getMetadata().getUid()).
                collect(Collectors.toSet());

        logger.info("Finished setting up test");
    }

    private static int zkPort(KafkaCluster cluster) {
        // TODO Method was added in DBZ-540, so no need for reflection once
        // dependency gets upgraded
        try {
            Field zkServerField = KafkaCluster.class.getDeclaredField("zkServer");
            zkServerField.setAccessible(true);
            return ((ZookeeperServer) zkServerField.get(cluster)).getPort();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void teardown(TestContext context) {
        logger.info("Tearing down test");
        Async async = context.async();
        if (deploymentId != null) {
            vertx.undeploy(deploymentId, ar -> {
                deploymentId = null;
                adminClient = null;
                kafka = null;
                k8s = null;
                controller = null;
                topicsConfigWatcher = null;
                topicsWatcher = null;
                if (ar.failed()) {
                    logger.error("Error undeploying session", ar.cause());
                    context.fail("Error undeploying session");
                }
                async.complete();
            });
        }
        async.await();
        if (kafkaCluster != null) {
            kafkaCluster.shutdown();
        }
        Runtime.getRuntime().removeShutdownHook(kafkaHook);
        logger.info("Finished tearing down test");
    }


    private ConfigMap createCm(TestContext context, ConfigMap cm) {
        String topicName = new TopicName(cm).toString();
        // Create a CM
        kubeClient.configMaps().create(cm);

        // Wait for the topic to be created
        waitFor(context, ()-> {
            try {
                adminClient.describeTopics(singletonList(topicName)).values().get(topicName).get();
                return true;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                    return false;
                } else {
                    throw new RuntimeException(e);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, timeout, "Expected topic to be created by now");
        return cm;
    }

    private ConfigMap createCm(TestContext context, String topicName) {
        Topic topic = new Topic.Builder(topicName, 1, (short) 1, emptyMap()).build();
        ConfigMap cm = TopicSerialization.toConfigMap(topic, cmPredicate);
        return createCm(context, cm);
    }

    private String createTopic(TestContext context, String topicName) throws InterruptedException, ExecutionException {
        logger.info("Creating topic {}", topicName);
        // Create a topic
        String configMapName = new TopicName(topicName).asMapName().toString();
        CreateTopicsResult crt = adminClient.createTopics(singletonList(new NewTopic(topicName, 1, (short) 1)));
        crt.all().get();

        // Wait for the configmap to be created
        waitFor(context, () -> {
            ConfigMap cm = kubeClient.configMaps().withName(configMapName).get();
            logger.info("Polled configmap {} waiting for creation", configMapName);
            return cm != null;
        }, timeout, "Expected the configmap to have been created by now");

        logger.info("configmap {} has been created", configMapName);
        return configMapName;
    }

    private void alterTopicConfig(TestContext context, String topicName, String configMapName) throws InterruptedException, ExecutionException {
        // Get the topic config
        ConfigResource configResource = topicConfigResource(topicName);
        org.apache.kafka.clients.admin.Config config = getTopicConfig(configResource);

        String key = "compression.type";

        Map<String, ConfigEntry> m = new HashMap<>();
        for (ConfigEntry entry: config.entries()) {
            m.put(entry.name(), entry);
        }
        final String changedValue;
        if ("snappy".equals(m.get(key).value())) {
            changedValue = "lz4";
        } else {
            changedValue = "snappy";
        }
        m.put(key, new ConfigEntry(key, changedValue));
        logger.info("Changing topic config {} to {}", key, changedValue);

        // Update the topic config
        AlterConfigsResult cgf = adminClient.alterConfigs(singletonMap(configResource,
                new org.apache.kafka.clients.admin.Config(m.values())));
        cgf.all().get();

        // Wait for the configmap to be modified
        waitFor(context, () -> {
            ConfigMap cm = kubeClient.configMaps().withName(configMapName).get();
            logger.info("Polled configmap {}, waiting for config change", configMapName);
            String gotValue = TopicSerialization.fromConfigMap(cm).getConfig().get(key);
            logger.info("Got value {}", gotValue);
            return changedValue.equals(gotValue);
        }, timeout, "Expected the configmap to have been deleted by now");
    }

    private org.apache.kafka.clients.admin.Config getTopicConfig(ConfigResource configResource) {
        try {
            return adminClient.describeConfigs(singletonList(configResource)).values().get(configResource).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private ConfigResource topicConfigResource(String topicName) {
        return new ConfigResource(ConfigResource.Type.TOPIC, topicName);
    }

    private void createAndAlterTopicConfig(TestContext context, String topicName) throws InterruptedException, ExecutionException {
        String configMapName = createTopic(context, topicName);
        alterTopicConfig(context, topicName, configMapName);
    }

    private void deleteTopic(TestContext context, String topicName, String configMapName) throws InterruptedException, ExecutionException {
        logger.info("Deleting topic {} (ConfigMap {})", topicName, configMapName);
        // Now we can delete the topic
        DeleteTopicsResult dlt = adminClient.deleteTopics(singletonList(topicName));
        dlt.all().get();
        logger.info("Deleted topic {}", topicName);

        // Wait for the configmap to be deleted
        waitFor(context, () -> {
            ConfigMap cm = kubeClient.configMaps().withName(configMapName).get();
            logger.info("Polled configmap {}, got {}, waiting for deletion", configMapName, cm);
            return cm == null;
        }, timeout, "Expected the configmap to have been deleted by now");
    }

    private void createAndDeleteTopic(TestContext context, String topicName) throws InterruptedException, ExecutionException {
        String configMapName = createTopic(context, topicName);
        deleteTopic(context, topicName, configMapName);
    }


    private void waitFor(TestContext context, BooleanSupplier ready, long timeout, String message) {
        Async async = context.async();
        long t0 = System.currentTimeMillis();
        Future<Void> fut = Future.future();
        vertx.setPeriodic(3_000L, timerId-> {
            // Wait for a configmap to be created
            boolean isFinished;
            try {
                isFinished = ready.getAsBoolean();
                if (isFinished) {
                    fut.complete();
                }
            } catch (Throwable e) {
                fut.fail(e);
                isFinished = true;
            }
            if (isFinished) {
                async.complete();
                vertx.cancelTimer(timerId);
            }
            long timeLeft = timeout - (System.currentTimeMillis() - t0);
            if (timeLeft <= 0) {
                vertx.cancelTimer(timerId);
                context.fail(message);
            }
        });
        async.await(timeout);
        if (fut.failed()) {
            context.fail(fut.cause());
        }
    }

    private void waitForEvent(TestContext context, ConfigMap cm, String expectedMessage, Controller.EventType expectedType) {
        waitFor(context, () -> {
            List<Event> items = kubeClient.events().withLabels(cmPredicate.labels()).list().getItems();
            List<Event> filtered = items.stream().
                    filter(evt -> !preExistingEvents.contains(evt.getMetadata().getUid())
                    && "ConfigMap".equals(evt.getInvolvedObject().getKind())
                    && cm.getMetadata().getName().equals(evt.getInvolvedObject().getName())).
                    collect(Collectors.toList());
            logger.debug("Waiting for events: {}", filtered.stream().map(evt->evt.getMessage()).collect(Collectors.toList()));
            if (!filtered.isEmpty()) {
                assertEquals(1, filtered.size());
                Event event = filtered.get(0);

                assertEquals(expectedMessage, event.getMessage());
                assertEquals(expectedType.name, event.getType());
                assertNotNull(event.getInvolvedObject());
                assertEquals("ConfigMap", event.getInvolvedObject().getKind());
                assertEquals(cm.getMetadata().getName(), event.getInvolvedObject().getName());
                return true;
            } else {
                return false;
            }
        }, timeout, "Expected an error event");
    }


    @Test
    public void testTopicAdded(TestContext context) throws Exception {
        createTopic(context, "test-topic-added");
    }

    @Test
    public void testTopicAddedWithEncodableName(TestContext context) throws Exception {
        createTopic(context, "thest-TOPIC_ADDED");
    }

    @Test
    public void testTopicDeleted(TestContext context) throws Exception {
        createAndDeleteTopic(context, "test-topic-deleted");
    }

    @Test
    public void testTopicDeletedWithEncodableName(TestContext context) throws Exception {
        createAndDeleteTopic(context, "test-TOPIC_DELETED");
    }

    @Test
    public void testTopicConfigChanged(TestContext context) throws Exception {
        createAndAlterTopicConfig(context, "test-topic-config-changed");
    }

    @Test
    public void testTopicConfigChangedWithEncodableName(TestContext context) throws Exception {
        createAndAlterTopicConfig(context, "test-TOPIC_CONFIG_CHANGED");
    }

    @Test
    @Ignore
    public void testTopicNumPartitionsChanged(TestContext context) {
        context.fail("Implement this");
    }

    @Test
    @Ignore
    public void testTopicNumReplicasChanged(TestContext context) {
        context.fail("Implement this");
    }

    @Test
    public void testConfigMapAdded(TestContext context) {
        String topicName = "test-configmap-created";
        createCm(context, topicName);
    }

    @Test
    public void testConfigMapAddedWithBadData(TestContext context) {
        String topicName = "test-configmap-created-with-bad-data";
        Topic topic = new Topic.Builder(topicName, 1, (short) 1, emptyMap()).build();
        ConfigMap cm = TopicSerialization.toConfigMap(topic, cmPredicate);
        cm.getData().put(TopicSerialization.CM_KEY_PARTITIONS, "foo");

        // Create a CM
        kubeClient.configMaps().create(cm);

        // Wait for the warning event
        waitForEvent(context, cm,
                "ConfigMap test-configmap-created-with-bad-data has an invalid 'data' section: " +
                        "ConfigMap's 'data' section has invalid key 'partitions': " +
                        "should be a strictly positive integer but was 'foo'",
                Controller.EventType.WARNING);

    }

    @Test
    public void testConfigMapDeleted(TestContext context) {
        // create the cm
        String topicName = "test-configmap-deleted";
        ConfigMap cm = createCm(context, topicName);

        // can now delete the cm
        kubeClient.configMaps().withName(cm.getMetadata().getName()).delete();

        // Wait for the topic to be deleted
        waitFor(context, ()-> {
            try {
                adminClient.describeTopics(singletonList(topicName)).values().get(topicName).get();
                return false;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                    return true;
                } else {
                    throw new RuntimeException(e);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, timeout, "Expected topic to be deleted by now");

    }


    @Test
    public void testConfigMapModifiedRetentionChanged(TestContext context) throws Exception {
        // create the cm
        String topicName = "test-configmap-modified-retention-changed";
        ConfigMap cm = createCm(context, topicName);

        // now change the cm
        kubeClient.configMaps().withName(cm.getMetadata().getName()).edit().addToData(TopicSerialization.CM_KEY_CONFIG, "{\"retention.ms\":\"12341234\"}").done();

        // Wait for that to be reflected in the topic
        waitFor(context, ()-> {
            ConfigResource configResource = topicConfigResource(topicName);
            org.apache.kafka.clients.admin.Config config = getTopicConfig(configResource);
            String retention = config.get("retention.ms").value();
            logger.debug("retention of {}, waiting for 12341234", retention);
            return "12341234".equals(retention);
        },  timeout, "Expected the topic to be updated");
    }

    @Test
    public void testConfigMapModifiedWithBadData(TestContext context) throws Exception {
        // create the cm
        String topicName = "test-configmap-modified-with-bad-data";
        ConfigMap cm = createCm(context, topicName);

        // now change the cm
        kubeClient.configMaps().withName(cm.getMetadata().getName()).edit().addToData(TopicSerialization.CM_KEY_PARTITIONS, "foo").done();

        // Wait for that to be reflected in the topic
        waitForEvent(context, cm,
                "ConfigMap test-configmap-modified-with-bad-data has an invalid 'data' section: " +
                        "ConfigMap's 'data' section has invalid key 'partitions': " +
                        "should be a strictly positive integer but was 'foo'",
                Controller.EventType.WARNING);

        // TODO Check we can change the partitions back and that future changes get handled correctly
        // TODO What if the topic end gets changed while it's in a broken state?
    }

    @Test
    public void testConfigMapModifiedNameChanged(TestContext context) throws Exception {
        // create the cm
        String topicName = "test-configmap-modified-name-changed";
        ConfigMap cm = createCm(context, topicName);

        // now change the cm
        String changedName = topicName.toUpperCase(Locale.ENGLISH);
        logger.info("Changing CM data.name from {} to {}", topicName, changedName);
        kubeClient.configMaps().withName(cm.getMetadata().getName()).edit().addToData(TopicSerialization.CM_KEY_NAME, changedName).done();

        // We expect this to cause a warning event
        waitForEvent(context, cm,
                "Kafka topics cannot be renamed, but ConfigMap's data.name has changed.",
                Controller.EventType.WARNING);

        // TODO Check we can change the data.name back and that future changes get handled correctly
        // TODO What if the topic end gets changed while the data.name is wrong?
    }

    // TODO: Test create with bogus cm.data
    // TODO: Test update with bogus cm.data
    // TODO: What happens if we create and then change labels to the CM predicate isn't matched any more
    //       What then happens if we change labels back?
    // TODO: Test create with CM metadata/name=bar,data/name=foo, then create another with metadata/name=foo,data/name=foo (expect error)
    //       How do we know (e.g. on failover) which one we were ignoring? Otherwise we could end up flip-flopping
    // TODO: Test create with CM metadata/name=bar,data/name=foo, then create another with metadata/name=foo (expect error)
    // TODO: Test create with CM metadata/name=foo, then create another with metadata/name=bar,data/name=foo (expect error)
}
