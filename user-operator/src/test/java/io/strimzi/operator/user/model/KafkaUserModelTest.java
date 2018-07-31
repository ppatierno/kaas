/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.user.model;

import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserSpec;
import io.strimzi.api.kafka.model.KafkaUserTlsClientAuthentication;
import io.strimzi.operator.common.model.Labels;
import io.strimzi.operator.common.operator.MockCertManager;
import io.strimzi.operator.user.ResourceUtils;

import java.util.Base64;

import io.fabric8.kubernetes.api.model.Secret;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class KafkaUserModelTest {
    private static KafkaUser user = ResourceUtils.createKafkaUser();
    private static Secret clientsCa = ResourceUtils.createClientsCa();
    private static Secret userCert = ResourceUtils.createUserCert();

    @Test
    public void testFromCrd()   {
        KafkaUserModel model = KafkaUserModel.fromCrd(new MockCertManager(), user, clientsCa, null);

        assertEquals(ResourceUtils.namespace, model.namespace);
        assertEquals(ResourceUtils.name, model.name);
        assertEquals(Labels.userLabels(ResourceUtils.labels).withKind(KafkaUser.RESOURCE_KIND), model.labels);
        assertEquals(KafkaUserTlsClientAuthentication.TYPE_TLS, model.authentication.getType());
    }

    @Test
    public void testGenerateSecret()    {
        KafkaUserModel model = KafkaUserModel.fromCrd(new MockCertManager(), user, clientsCa, null);
        Secret generated = model.generateSecret();

        System.out.println(generated.getData().keySet());

        assertEquals(ResourceUtils.name, generated.getMetadata().getName());
        assertEquals(ResourceUtils.namespace, generated.getMetadata().getNamespace());
        assertEquals(Labels.userLabels(ResourceUtils.labels).withKind(KafkaUser.RESOURCE_KIND).toMap(), generated.getMetadata().getLabels());
    }

    @Test
    public void testGenerateCertificateWhenNoExists()    {
        KafkaUserModel model = KafkaUserModel.fromCrd(new MockCertManager(), user, clientsCa, null);
        Secret generated = model.generateSecret();

        assertEquals("clients-ca-crt", new String(model.decodeFromSecret(generated, "ca.crt")));
        assertEquals("crt file", new String(model.decodeFromSecret(generated, "user.crt")));
        assertEquals("key file", new String(model.decodeFromSecret(generated, "user.key")));
    }

    @Test
    public void testGenerateCertificateAtCaChange()    {
        Secret clientsCa = ResourceUtils.createClientsCa();
        clientsCa.getData().put("clients-ca.key", Base64.getEncoder().encodeToString("different-clients-ca-key".getBytes()));
        clientsCa.getData().put("clients-ca.crt", Base64.getEncoder().encodeToString("different-clients-ca-crt".getBytes()));

        KafkaUserModel model = KafkaUserModel.fromCrd(new MockCertManager(), user, clientsCa, userCert);
        Secret generated = model.generateSecret();

        assertEquals("different-clients-ca-crt", new String(model.decodeFromSecret(generated, "ca.crt")));
        assertEquals("crt file", new String(model.decodeFromSecret(generated, "user.crt")));
        assertEquals("key file", new String(model.decodeFromSecret(generated, "user.key")));
    }

    @Test
    public void testGenerateCertificateKeepExisting()    {
        KafkaUserModel model = KafkaUserModel.fromCrd(new MockCertManager(), user, clientsCa, userCert);
        Secret generated = model.generateSecret();

        assertEquals("clients-ca-crt", new String(model.decodeFromSecret(generated, "ca.crt")));
        assertEquals("expected-crt", new String(model.decodeFromSecret(generated, "user.crt")));
        assertEquals("expected-key", new String(model.decodeFromSecret(generated, "user.key")));
    }

    @Test
    public void testNoTlsAuth()    {
        KafkaUser user = ResourceUtils.createKafkaUser();
        user.setSpec(new KafkaUserSpec());
        KafkaUserModel model = KafkaUserModel.fromCrd(new MockCertManager(), user, clientsCa, userCert);

        assertNull(model.generateSecret());
    }
}
