import { KeycloakConfig } from 'keycloak-angular';

let keycloakConfiguration: KeycloakConfig = {
  url: 'http://localhost:8443/auth',
  realm: 'PartnerRegistry',
  clientId: 'portal'
};

export const environment = {
  production: true,
  keycloakConfig: keycloakConfiguration
};
