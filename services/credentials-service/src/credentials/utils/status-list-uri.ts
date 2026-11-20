// Single source for the IETF Token Status List token URL of a status list
// (draft-ietf-oauth-status-list). The SD-JWT `status.status_list.uri` claim
// points here, and the token endpoint uses the same URL as the token's
// `sub`, so the two can never drift apart.
//
// The list id is the status list credential's id (a did:rcw:... DID). It
// cannot itself be the claim's uri: it is not an HTTPS URL and resolves to
// the W3C RevocationList2020 credential, which Token Status List verifiers
// cannot parse — hence this HTTPS token endpoint.
//
// The base URL must be reachable by external verifiers, so it prefers
// CREDENTIAL_SERVICE_PUBLIC_URL — the service's public HTTPS base URL.
// CREDENTIAL_SERVICE_BASE_URL is only a fallback: in the Docker deployment
// it is the in-network address (http://credential:3000), which verifiers
// outside the deployment cannot resolve or reach.
export function statusListTokenUri(statusListCredential: string): string {
  const baseUrl =
    process.env.CREDENTIAL_SERVICE_PUBLIC_URL ||
    process.env.CREDENTIAL_SERVICE_BASE_URL;
  return `${baseUrl}/credentials/status-list/${statusListCredential}/token`;
}
