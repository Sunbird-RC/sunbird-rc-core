import 'server-only'

function required(name: string, fallback?: string): string {
  const v = process.env[name] ?? fallback
  if (v === undefined) throw new Error(`Missing required env var: ${name}`)
  return v
}

function bool(name: string, fallback: boolean): boolean {
  const v = process.env[name]
  if (v === undefined) return fallback
  return v === 'true' || v === '1'
}

export const env = {
  registryBaseUrl: required('REGISTRY_BASE_URL', 'http://registry:8081'),
  claimMsBaseUrl: required('CLAIM_MS_BASE_URL', 'http://claim-ms:8082'),
  credentialSchemaBaseUrl: required('CREDENTIAL_SCHEMA_BASE_URL', 'http://credential-schema:3333'),
  credentialsBaseUrl: required('CREDENTIALS_BASE_URL', 'http://credential:3000'),
  identityBaseUrl: required('IDENTITY_BASE_URL', 'http://identity:3332'),
  oid4vcBaseUrl: required('OID4VC_BASE_URL', 'http://oid4vc-service:3400'),

  // Real backend flags, mirrored 1:1 from the same env vars the backend
  // containers read (docker-compose.yml). Never invent separate admin-ui
  // toggles for these — one flip should affect backend and UI gating together.
  flags: {
    claims: bool('CLAIMS_ENABLED', false),
    // Repo's own env var name has this typo (FILESSTORAGE, not FILESTORAGE) —
    // match it exactly rather than "fixing" it locally, or the flag silently
    // never lines up with the backend's actual state.
    filestorage: bool('FILESSTORAGE_ENABLED', false),
    authentication: bool('AUTHENTICATION_ENABLED', false),
    idgen: bool('IDGEN_ENABLED', false),
    signature: bool('SIGNATURE_ENABLED', false),
  },

  adminUiAuthEnabled: bool('ADMIN_UI_AUTH_ENABLED', true),
  keycloakIssuer: process.env.KEYCLOAK_ISSUER,
}
