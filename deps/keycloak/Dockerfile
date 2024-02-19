ARG KEYCLOAK_BASE_IMAGE=sunbirdrc/keycloak
FROM $KEYCLOAK_BASE_IMAGE

ADD themes /opt/jboss/keycloak/themes
ADD providers /opt/jboss/keycloak/providers

ENTRYPOINT ["/opt/jboss/tools/docker-entrypoint.sh"]
