#!/usr/bin/env bash
set -e

# Parameters:
# $1: Listener ID
# $2: Broker ID
# $3: List of options
function get_option_for_broker {
  for OPTION in $3 ; do
    if [[ $OPTION == "$1_$2"://* ]] ; then
      echo "${OPTION#$1_$2://}"
      break
    fi
  done
}

# Get broker rack if it's enabled from the file $KAFKA_HOME/init/rack.id (if it exists). This file is generated by the
# init-container used when rack awareness is enabled.
if [ -e "$KAFKA_HOME/init/rack.id" ]; then
  STRIMZI_RACK_ID=$(cat "$KAFKA_HOME/init/rack.id")
  export STRIMZI_RACK_ID
fi

# List for keeping track of env vars which should be substituted in the configuration
# shellcheck disable=SC2016
SUBSTITUTIONS='${STRIMZI_BROKER_ID},${STRIMZI_RACK_ID},${CERTS_STORE_PASSWORD}'

# If there are any node port listeners, the file $KAFKA_HOME/init/external.address will contain definitions of env vars
# with their address for the substitution
NODE_PORT_CONFIG_FILE=$KAFKA_HOME/init/external.address
if [ -e "$NODE_PORT_CONFIG_FILE" ]; then
  # shellcheck source=/opt/kafka/init/external.address
  # shellcheck disable=SC1091
  source "${NODE_PORT_CONFIG_FILE}"
fi

ADVERTISED_HOSTNAMES_LIST="$(cat "$KAFKA_HOME/custom-config/advertised-hostnames.config")"
ADVERTISED_PORT_LIST="$(cat "$KAFKA_HOME/custom-config/advertised-ports.config")"

# Get through all listeners and set the required environment variables for them
LISTENERS=$(cat "$KAFKA_HOME/custom-config/listeners.config")
for LISTENER in $LISTENERS ; do
  # Find the advertised hostname for this broker passed by the operator
  VAR_NAME="STRIMZI_${LISTENER}_ADVERTISED_HOSTNAME"
  ADVERTISED_HOSTNAME="$(get_option_for_broker "$LISTENER" "$STRIMZI_BROKER_ID" "$ADVERTISED_HOSTNAMES_LIST")"

  # shellcheck disable=SC2076
  if [[ "${ADVERTISED_HOSTNAME}" =~ "\$${*}" ]]; then
    ADVERTISED_HOSTNAME="${ADVERTISED_HOSTNAME:2:-1}"
    declare "$VAR_NAME"="${!ADVERTISED_HOSTNAME}"
  else
    declare "$VAR_NAME"="${ADVERTISED_HOSTNAME}"
  fi

  export "${VAR_NAME?}"
  SUBSTITUTIONS="$SUBSTITUTIONS,\${STRIMZI_${LISTENER}_ADVERTISED_HOSTNAME}"

  # Find the external port for this broker
  VAR_NAME="STRIMZI_${LISTENER}_ADVERTISED_PORT"
  declare "$VAR_NAME"="$(get_option_for_broker "$LISTENER" "$STRIMZI_BROKER_ID" "$ADVERTISED_PORT_LIST")"
  export "${VAR_NAME?}"
  SUBSTITUTIONS="$SUBSTITUTIONS,\${STRIMZI_${LISTENER}_ADVERTISED_PORT}"

  # If OAuth is used, add the environment variables to the list for the envsubst command
  VAR_NAME="STRIMZI_${LISTENER}_OAUTH_CLIENT_SECRET"
  if [ -z "${!VAR_NAME}" ]; then
    SUBSTITUTIONS="$SUBSTITUTIONS,\${STRIMZI_${LISTENER}_OAUTH_CLIENT_SECRET}"
  fi
done

# shellcheck disable=SC2016
envsubst \
    "$SUBSTITUTIONS" \
    < "$KAFKA_HOME/custom-config/server.config"
