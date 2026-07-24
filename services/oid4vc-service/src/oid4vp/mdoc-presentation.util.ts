import { cborEncode, DataItem } from '@auth0/mdl/lib/cbor';
import { Verifier } from '@auth0/mdl';

// OID4VP's mdoc profile (ISO/IEC 18013-7 Annex B) SessionTranscript: a
// Tag-24-wrapped CBOR array
// [null, null, [mdocGeneratedNonce, clientId, responseUri, verifierGeneratedNonce]] —
// byte-identical to what @auth0/mdl's own DeviceResponse.usingSessionTranscriptForOID4VP()
// builds on the wallet side, using the same cborEncode/DataItem utilities.
//
// A real encrypted-response (direct_post.jwt) OID4VP-mdoc flow carries
// mdocGeneratedNonce in the JWE `apu` header; this deployment uses plain
// (unencrypted) direct_post like the other 3 formats, so there's no such
// channel. The wallet instead sends it explicitly as `mdoc_generated_nonce`
// in the /vp/response body, and that same value is used here to reconstruct
// byte-identical transcript bytes — a deliberate, documented simplification
// for this deployment's convention, not a full ISO 18013-7 Annex B profile.
export function buildSessionTranscript(
  mdocGeneratedNonce: string,
  clientId: string,
  responseUri: string,
  verifierGeneratedNonce: string,
): Buffer {
  return cborEncode(
    DataItem.fromData([null, null, [mdocGeneratedNonce, clientId, responseUri, verifierGeneratedNonce]]),
  );
}

export interface MdocPresentationResult {
  verified: boolean;
  documents: Array<{ docType: string; claims: Record<string, Record<string, any>> }>;
  error?: string;
}

// Verifies a CBOR DeviceResponse (base64url-encoded vp_token) — issuer
// signature, per-item digests, AND device signature/binding against the
// given SessionTranscript, all via @auth0/mdl's own Verifier. Public-key-only
// (issuer cert + device key, both embedded in the credential/response), so
// this runs directly here rather than round-tripping to identity-service —
// consistent with how holder-JWT verification already happens inline in
// oid4vp.service.ts, with identity-service reserved for private-key
// (signing) operations only.
export async function verifyMdocPresentation(
  deviceResponseBase64url: string,
  sessionTranscriptBytes: Buffer,
): Promise<MdocPresentationResult> {
  let bytes: Buffer;
  try {
    bytes = Buffer.from(deviceResponseBase64url, 'base64url');
  } catch (err) {
    return { verified: false, documents: [], error: `Malformed DeviceResponse: ${err}` };
  }
  const failures: any[] = [];
  const onCheck = (result: any) => {
    if (result.status === 'FAILED') failures.push(result);
  };
  const verifier: any = new Verifier([]);
  let parsed: any;
  try {
    parsed = await verifier.verify(bytes, {
      encodedSessionTranscript: sessionTranscriptBytes,
      disableCertificateChainValidation: true,
      onCheck,
    });
  } catch (err) {
    return { verified: false, documents: [], error: `${err}` };
  }
  if (failures.length) {
    return { verified: false, documents: [], error: failures.map((f: any) => f.check).join('; ') };
  }
  const documents = (parsed.documents || []).map((doc: any) => {
    const claims: Record<string, Record<string, any>> = {};
    for (const ns of doc.issuerSignedNameSpaces) {
      claims[ns] = doc.getIssuerNameSpace(ns);
    }
    return { docType: doc.docType, claims };
  });
  return { verified: true, documents };
}
