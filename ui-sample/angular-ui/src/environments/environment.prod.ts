import { KeycloakConfig } from 'keycloak-angular';

let keycloakConfiguration: KeycloakConfig = {
  url: 'http://localhost:8080/auth',
  realm: 'PartnerRegistry',
  clientId: 'portal',
  "credentials": {
    "secret": "" 
  }  
};

export const environment = {
  production: true,
  keycloakConfig: keycloakConfiguration
};
