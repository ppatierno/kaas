/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;

import io.strimzi.api.kafka.model.connect.build.Artifact;
import io.strimzi.api.kafka.model.connect.build.Build;
import io.strimzi.api.kafka.model.connect.build.BuildBuilder;
import io.strimzi.api.kafka.model.connect.build.JarArtifactBuilder;
import io.strimzi.api.kafka.model.connect.build.OtherArtifactBuilder;
import io.strimzi.api.kafka.model.connect.build.PluginBuilder;
import io.strimzi.api.kafka.model.connect.build.TgzArtifactBuilder;
import io.strimzi.api.kafka.model.connect.build.ZipArtifactBuilder;
import io.strimzi.test.annotations.ParallelSuite;
import io.strimzi.test.annotations.ParallelTest;

import static io.strimzi.operator.cluster.model.KafkaBrokerConfigurationBuilderTest.IsEquivalent.isEquivalent;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ParallelSuite
public class KafkaConnectDockerfileTest {
    private final Artifact jarArtifactNoChecksum = new JarArtifactBuilder()
            .withUrl("https://mydomain.tld/my.jar")
            .build();

    private final Artifact jarArtifactWithChecksum = new JarArtifactBuilder()
            .withUrl("https://mydomain.tld/my2.jar")
            .withSha512sum("sha-512-checksum")
            .build();

    private final Artifact otherArtifactNoChecksum = new OtherArtifactBuilder()
            .withUrl("https://mydomain.tld/download?artifact=my.so")
            .withFileName("my.so")
            .build();

    private final Artifact otherArtifactWithChecksum = new OtherArtifactBuilder()
            .withUrl("https://mydomain.tld/download?artifactId=1874")
            .withSha512sum("sha-512-checksum")
            .withFileName("my2.so")
            .build();

    private final Artifact otherArtifactWithChecksumWithoutName = new OtherArtifactBuilder()
            .withUrl("https://mydomain.tld/download?artifactId=1874")
            .withSha512sum("sha-512-checksum")
            .build();

    private final Artifact tgzArtifactNoChecksum = new TgzArtifactBuilder()
            .withUrl("https://mydomain.tld/my.tgz")
            .build();

    private final Artifact zipArtifactNoChecksum = new ZipArtifactBuilder()
            .withUrl("https://mydomain.tld/my.zip")
            .build();

    private final Artifact tgzArtifactWithChecksum = new TgzArtifactBuilder()
            .withUrl("https://mydomain.tld/my2.tgz")
            .withSha512sum("sha-512-checksum")
            .build();

    private final Artifact zipArtifactWithChecksum = new ZipArtifactBuilder()
            .withUrl("https://mydomain.tld/my2.zip")
            .withSha512sum("sha-512-checksum")
            .build();

    @ParallelTest
    public void testEmptyDockerfile()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(emptyList())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "USER 1001"));
    }

    @ParallelTest
    public void testNoArtifacts()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(emptyList())
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "USER 1001"));
    }

    @ParallelTest
    public void testNoChecksumJarArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(jarArtifactNoChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/51e5038c \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/51e5038c/51e5038c.jar https://mydomain.tld/my.jar",
                "USER 1001"));
    }

    @ParallelTest
    public void testChecksumJarArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(jarArtifactWithChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/0df6d15c \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar https://mydomain.tld/my2.jar \\",
                "      && echo \"sha-512-checksum /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar\" > /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar.sha512 \\",
                "      && sha512sum --check /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar.sha512 \\",
                "      && rm -f /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar.sha512",
                "USER 1001"));
    }

    @ParallelTest
    public void testNoChecksumOtherArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(otherArtifactNoChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/2c3b64c7 \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/2c3b64c7/my.so https://mydomain.tld/download?artifact=my.so",
                "USER 1001"));
    }

    @ParallelTest
    public void testChecksumOtherArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(otherArtifactWithChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/2e6fee06 \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/2e6fee06/my2.so https://mydomain.tld/download?artifactId=1874 \\",
                "      && echo \"sha-512-checksum /opt/kafka/plugins/my-connector-plugin/2e6fee06/my2.so\" > /opt/kafka/plugins/my-connector-plugin/2e6fee06/my2.so.sha512 \\",
                "      && sha512sum --check /opt/kafka/plugins/my-connector-plugin/2e6fee06/my2.so.sha512 \\",
                "      && rm -f /opt/kafka/plugins/my-connector-plugin/2e6fee06/my2.so.sha512",
                "USER 1001"));
    }

    @ParallelTest
    public void testChecksumOtherArtifactWithoutName()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(otherArtifactWithChecksumWithoutName)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/2e6fee06 \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/2e6fee06/2e6fee06 https://mydomain.tld/download?artifactId=1874 \\",
                "      && echo \"sha-512-checksum /opt/kafka/plugins/my-connector-plugin/2e6fee06/2e6fee06\" > /opt/kafka/plugins/my-connector-plugin/2e6fee06/2e6fee06.sha512 \\",
                "      && sha512sum --check /opt/kafka/plugins/my-connector-plugin/2e6fee06/2e6fee06.sha512 \\",
                "      && rm -f /opt/kafka/plugins/my-connector-plugin/2e6fee06/2e6fee06.sha512",
                "USER 1001"));
    }

    @ParallelTest
    public void testMultipleJarArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(jarArtifactNoChecksum, jarArtifactWithChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/51e5038c \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/51e5038c/51e5038c.jar https://mydomain.tld/my.jar",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/0df6d15c \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar https://mydomain.tld/my2.jar \\",
                "      && echo \"sha-512-checksum /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar\" > /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar.sha512 \\",
                "      && sha512sum --check /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar.sha512 \\",
                "      && rm -f /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar.sha512",
                "USER 1001"));
    }

    @ParallelTest
    public void testNoChecksumTgzArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(tgzArtifactNoChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/6718766b \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/6718766b.tgz https://mydomain.tld/my.tgz \\",
                "      && tar xvfz /opt/kafka/plugins/my-connector-plugin/6718766b.tgz -C /opt/kafka/plugins/my-connector-plugin/6718766b \\",
                "      && rm -vf /opt/kafka/plugins/my-connector-plugin/6718766b.tgz",
                "USER 1001"));
    }

    @ParallelTest
    public void testNoChecksumZipArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(zipArtifactNoChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/d8d533bc \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/d8d533bc.zip https://mydomain.tld/my.zip \\",
                "      && unzip /opt/kafka/plugins/my-connector-plugin/d8d533bc.zip -d /opt/kafka/plugins/my-connector-plugin/d8d533bc \\",
                "      && find /opt/kafka/plugins/my-connector-plugin/d8d533bc -type l | xargs rm -f \\",
                "      && rm -vf /opt/kafka/plugins/my-connector-plugin/d8d533bc.zip",
                "USER 1001"));
    }

    @ParallelTest
    public void testChecksumZipArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(zipArtifactWithChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/90e04094 \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/90e04094.zip https://mydomain.tld/my2.zip \\",
                "      && echo \"sha-512-checksum /opt/kafka/plugins/my-connector-plugin/90e04094.zip\" > /opt/kafka/plugins/my-connector-plugin/90e04094.zip.sha512 \\",
                "      && sha512sum --check /opt/kafka/plugins/my-connector-plugin/90e04094.zip.sha512 \\",
                "      && rm -f /opt/kafka/plugins/my-connector-plugin/90e04094.zip.sha512 \\",
                "      && unzip /opt/kafka/plugins/my-connector-plugin/90e04094.zip -d /opt/kafka/plugins/my-connector-plugin/90e04094 \\",
                "      && find /opt/kafka/plugins/my-connector-plugin/90e04094 -type l | xargs rm -f \\",
                "      && rm -vf /opt/kafka/plugins/my-connector-plugin/90e04094.zip",
                "USER 1001"));
    }

    @ParallelTest
    public void testChecksumTgzArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(tgzArtifactWithChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/638bd501 \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/638bd501.tgz https://mydomain.tld/my2.tgz \\",
                "      && echo \"sha-512-checksum /opt/kafka/plugins/my-connector-plugin/638bd501.tgz\" > /opt/kafka/plugins/my-connector-plugin/638bd501.tgz.sha512 \\",
                "      && sha512sum --check /opt/kafka/plugins/my-connector-plugin/638bd501.tgz.sha512 \\",
                "      && rm -f /opt/kafka/plugins/my-connector-plugin/638bd501.tgz.sha512 \\",
                "      && tar xvfz /opt/kafka/plugins/my-connector-plugin/638bd501.tgz -C /opt/kafka/plugins/my-connector-plugin/638bd501 \\",
                "      && rm -vf /opt/kafka/plugins/my-connector-plugin/638bd501.tgz",
                "USER 1001"));
    }

    @ParallelTest
    public void testMultipleTgzArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(tgzArtifactNoChecksum, tgzArtifactWithChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/6718766b \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/6718766b.tgz https://mydomain.tld/my.tgz \\",
                "      && tar xvfz /opt/kafka/plugins/my-connector-plugin/6718766b.tgz -C /opt/kafka/plugins/my-connector-plugin/6718766b \\",
                "      && rm -vf /opt/kafka/plugins/my-connector-plugin/6718766b.tgz",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/638bd501 \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/638bd501.tgz https://mydomain.tld/my2.tgz \\",
                "      && echo \"sha-512-checksum /opt/kafka/plugins/my-connector-plugin/638bd501.tgz\" > /opt/kafka/plugins/my-connector-plugin/638bd501.tgz.sha512 \\",
                "      && sha512sum --check /opt/kafka/plugins/my-connector-plugin/638bd501.tgz.sha512 \\",
                "      && rm -f /opt/kafka/plugins/my-connector-plugin/638bd501.tgz.sha512 \\",
                "      && tar xvfz /opt/kafka/plugins/my-connector-plugin/638bd501.tgz -C /opt/kafka/plugins/my-connector-plugin/638bd501 \\",
                "      && rm -vf /opt/kafka/plugins/my-connector-plugin/638bd501.tgz",
                "USER 1001"));
    }

    @ParallelTest
    public void testMultipleZipArtifact()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(zipArtifactNoChecksum, zipArtifactWithChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), isEquivalent("FROM myImage:latest",
                "USER root:root",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/d8d533bc \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/d8d533bc.zip https://mydomain.tld/my.zip \\",
                "      && unzip /opt/kafka/plugins/my-connector-plugin/d8d533bc.zip -d /opt/kafka/plugins/my-connector-plugin/d8d533bc \\",
                "      && find /opt/kafka/plugins/my-connector-plugin/d8d533bc -type l | xargs rm -f \\",
                "      && rm -vf /opt/kafka/plugins/my-connector-plugin/d8d533bc.zip",
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/90e04094 \\",
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/90e04094.zip https://mydomain.tld/my2.zip \\",
                "      && echo \"sha-512-checksum /opt/kafka/plugins/my-connector-plugin/90e04094.zip\" > /opt/kafka/plugins/my-connector-plugin/90e04094.zip.sha512 \\",
                "      && sha512sum --check /opt/kafka/plugins/my-connector-plugin/90e04094.zip.sha512 \\",
                "      && rm -f /opt/kafka/plugins/my-connector-plugin/90e04094.zip.sha512 \\",
                "      && unzip /opt/kafka/plugins/my-connector-plugin/90e04094.zip -d /opt/kafka/plugins/my-connector-plugin/90e04094 \\",
                "      && find /opt/kafka/plugins/my-connector-plugin/90e04094 -type l | xargs rm -f \\",
                "      && rm -vf /opt/kafka/plugins/my-connector-plugin/90e04094.zip",
                "USER 1001"));
    }

    @ParallelTest
    public void testDockerfileWithComments()   {
        Build connectBuild = new BuildBuilder()
                .withPlugins(new PluginBuilder()
                        .withName("my-connector-plugin")
                        .withArtifacts(jarArtifactNoChecksum, jarArtifactWithChecksum)
                        .build())
                .build();

        KafkaConnectDockerfile df = new KafkaConnectDockerfile("myImage:latest", connectBuild);

        assertThat(df.getDockerfile(), is("##############################\n" +
                "##############################\n" +
                "# This file is automatically generated by the Strimzi Cluster Operator\n" +
                "# Any changes to this file will be ignored and overwritten!\n" +
                "##############################\n" +
                "##############################\n" +
                "\n" +
                "FROM myImage:latest\n" +
                "\n" +
                "USER root:root\n" +
                "\n" +
                "##########\n" +
                "# Connector plugin my-connector-plugin\n" +
                "##########\n" +
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/51e5038c \\\n" +
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/51e5038c/51e5038c.jar https://mydomain.tld/my.jar\n" +
                "\n" +
                "RUN mkdir -p /opt/kafka/plugins/my-connector-plugin/0df6d15c \\\n" +
                "      && curl -L --output /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar https://mydomain.tld/my2.jar \\\n" +
                "      && echo \"sha-512-checksum /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar\" > /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar.sha512 \\\n" +
                "      && sha512sum --check /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar.sha512 \\\n" +
                "      && rm -f /opt/kafka/plugins/my-connector-plugin/0df6d15c/0df6d15c.jar.sha512\n" +
                "\n" +
                "USER 1001\n\n"));
    }
}
