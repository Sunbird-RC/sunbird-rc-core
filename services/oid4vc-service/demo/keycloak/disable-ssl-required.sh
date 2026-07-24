#!/usr/bin/env bash
# The legacy (WildFly) Keycloak's master realm requires HTTPS for requests that
# don't look local — and the host->container port hop looks non-local, so the
# admin REST calls in setup-keycloak.mjs get "HTTPS required". This flips
# sslRequired=NONE on the master + sunbird-rc realms via kcadm *inside* the
# container (where it is genuinely localhost). One-time, before setup-keycloak.
set -euo pipefail
KC="${KC_CONTAINER:-sunbird-rc-core-keycloak-1}"
REALM="${KC_REALM:-sunbird-rc}"
USER="${KC_ADMIN:-admin}"
PASS="${KC_ADMIN_PASSWORD:-admin123}"

IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$KC")
echo "Keycloak container $KC at $IP — disabling sslRequired on master + $REALM"
docker exec "$KC" /opt/jboss/keycloak/bin/kcadm.sh config credentials \
  --server "http://$IP:8080/auth" --realm master --user "$USER" --password "$PASS"
docker exec "$KC" /opt/jboss/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE
docker exec "$KC" /opt/jboss/keycloak/bin/kcadm.sh update "realms/$REALM" -s sslRequired=NONE
echo "done."
