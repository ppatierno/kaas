#!/bin/bash

# Prepare super.users field
KAFKA_NAME=$(hostname | rev | cut -d "-" -f2- | rev)
ASSEMBLY_NAME=$(echo "${KAFKA_NAME}" | rev | cut -d "-" -f2- | rev)
SUPER_USERS="super.users=User:CN=${ASSEMBLY_NAME}-topic-operator,O=io.strimzi;User:CN=${KAFKA_NAME},O=io.strimzi"

# Configuring TLS client authentication for clienttls interface
if [ "$KAFKA_CLIENTTLS_TLS_CLIENT_AUTHENTICATION" = "required" ]; then
  LISTENER_NAME_CLIENTTLS_SSL_CLIENT_AUTH="required"
elif [ "$KAFKA_CLIENTTLS_TLS_CLIENT_AUTHENTICATION" = "requested" ]; then
  LISTENER_NAME_CLIENTTLS_SSL_CLIENT_AUTH="requested"
else
  LISTENER_NAME_CLIENTTLS_SSL_CLIENT_AUTH="none"
fi

# Configuring authorization
if [ "$KAFKA_AUTHORIZER_TYPE" = "SimpleACLAuthorizer" ]; then
  AUTHORIZER_CLASS_NAME="kafka.security.auth.SimpleAclAuthorizer"
else
  AUTHORIZER_CLASS_NAME=""
fi

# Write the config file
cat <<EOF
broker.id=${KAFKA_BROKER_ID}
broker.rack=${KAFKA_RACK}

# Listeners
listeners=CLIENT://0.0.0.0:9092,REPLICATION://0.0.0.0:9091,CLIENTTLS://0.0.0.0:9093
advertised.listeners=CLIENT://$(hostname -f):9092,REPLICATION://$(hostname -f):9091,CLIENTTLS://$(hostname -f):9093
listener.security.protocol.map=CLIENT:PLAINTEXT,REPLICATION:SSL,CLIENTTLS:SSL
inter.broker.listener.name=REPLICATION

# Zookeeper
zookeeper.connect=localhost:2181
zookeeper.connection.timeout.ms=6000

# Logs
log.dirs=${KAFKA_LOG_DIRS}

# TLS / SSL
ssl.keystore.password=${CERTS_STORE_PASSWORD}
ssl.truststore.password=${CERTS_STORE_PASSWORD}
ssl.keystore.type=PKCS12
ssl.truststore.type=PKCS12

listener.name.replication.ssl.keystore.location=/tmp/kafka/cluster.keystore.p12
listener.name.replication.ssl.truststore.location=/tmp/kafka/cluster.truststore.p12
listener.name.replication.ssl.client.auth=required

listener.name.clienttls.ssl.keystore.location=/tmp/kafka/cluster.keystore.p12
listener.name.clienttls.ssl.truststore.location=/tmp/kafka/clients.truststore.p12
listener.name.clienttls.ssl.client.auth=${LISTENER_NAME_CLIENTTLS_SSL_CLIENT_AUTH}

# Authorization configuration
authorizer.class.name=${AUTHORIZER_CLASS_NAME}
${SUPER_USERS}

# Provided configuration
${KAFKA_CONFIGURATION}
EOF