// mso_mdoc (ISO/IEC 18013-5 / -7) presentation on the WALLET side.
//
// This is the piece the Postman collection cannot do (a sandbox can't build a
// CBOR DeviceResponse) and the main reason this wallet exists. The issued mdoc
// carries the holder's public key as deviceKeyInfo.deviceKey; here we sign a
// DeviceResponse with the matching private key over the OID4VP SessionTranscript.
//
// The transcript MUST be byte-identical to what the service rebuilds in
// oid4vp.service.ts via buildSessionTranscript() — both derive from
// @auth0/mdl's usingSessionTranscriptForOID4VP(mdocGeneratedNonce, clientId,
// responseUri, verifierGeneratedNonce), so we use that same builder here.
import { DeviceResponse, parse } from '@auth0/mdl';
import { b64urlToBytes, bytesToB64url } from './util.mjs';

// Build a PresentationDefinition selecting the requested elements. @auth0/mdl
// requires input_descriptor.id === docType and JSONPath fields of the form
// $['<namespace>']['<element>'] (see DeviceResponse.prepareDigest).
function buildPresentationDefinition(docType, fields) {
  return {
    id: 'wallet-mdoc-presentation',
    input_descriptors: [
      {
        id: docType,
        format: { mso_mdoc: { alg: ['ES256'] } },
        constraints: {
          limit_disclosure: 'required',
          fields: fields.map((f) => ({
            path: [`$['${f.namespace}']['${f.element}']`],
            intent_to_retain: false,
          })),
        },
      },
    ],
  };
}

// Given the issued mdoc (base64url CBOR) and the requested (namespace,element)
// fields from the DCQL query, produce a signed DeviceResponse (base64url) to
// send as the vp_token.
export async function buildMdocDeviceResponse(
  issuedMdocBase64url,
  holder,
  { docType, fields, mdocGeneratedNonce, clientId, responseUri, verifierNonce },
) {
  const issuerMdocBytes = b64urlToBytes(issuedMdocBase64url);
  const pd = buildPresentationDefinition(docType, fields);

  const deviceResponse = await DeviceResponse.from(issuerMdocBytes)
    .usingPresentationDefinition(pd)
    .usingSessionTranscriptForOID4VP(mdocGeneratedNonce, clientId, responseUri, verifierNonce)
    .authenticateWithSignature(holder.privateJwk, 'ES256')
    .sign();

  return bytesToB64url(new Uint8Array(deviceResponse.encode()));
}

// Parse the docType out of an issued mdoc (for display / DCQL matching).
export async function inspectMdoc(issuedMdocBase64url) {
  const mdoc = parse(b64urlToBytes(issuedMdocBase64url));
  const doc = mdoc.documents[0];
  const claims = {};
  for (const ns of doc.issuerSignedNameSpaces) {
    claims[ns] = doc.getIssuerNameSpace(ns);
  }
  return { docType: doc.docType, claims };
}
