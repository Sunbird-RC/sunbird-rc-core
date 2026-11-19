// Single source for the IETF Token Status List token URL of a status list
// (draft-ietf-oauth-status-list). The SD-JWT `status.status_list.uri` claim
// points here, and the token endpoint uses the same URL as the token's
// `sub`, so the two can never drift apart.
//
// The list id is the status list credential's id (a did:rcw:... DID). It
// cannot itself be the claim's uri: it is not an HTTPS URL and resolves to
// the W3C RevocationList2020 credential, which Token Status List verifiers
// cannot parse — hence this HTTPS token endpoint.
export function statusListTokenUri(statusListCredential: string): string {
  return `${process.env.CREDENTIAL_SERVICE_BASE_URL}/credentials/status-list/${statusListCredential}/token`;
}
