import { Injectable } from '@nestjs/common';

// SD-JWT VC's IANA-registered format identifier was renamed from `vc+sd-jwt`
// to `dc+sd-jwt` partway through the spec's drafts. Some wallets' DCQL
// parsers are a strict enum that only accepts one spelling and reject the
// other — but this codebase's OID4VCI side still (correctly, for its own
// draft target) publishes `vc+sd-jwt` as the credential format id. Treat
// both spellings as the same format for DCQL matching purposes so either a
// query or a presented credential can use either spelling without breaking
// the other side.
const SD_JWT_FORMAT_ALIASES = new Set(['vc+sd-jwt', 'dc+sd-jwt']);
function sameFormat(a: string, b: string): boolean {
  if (a === b) return true;
  return SD_JWT_FORMAT_ALIASES.has(a) && SD_JWT_FORMAT_ALIASES.has(b);
}

// Minimal DCQL (Digital Credentials Query Language, OID4VP 1.0) evaluator.
// Covers the common case: credential-set queries by type/vct + claim-path
// presence. Works over both ldp_vc (JSON-LD) and jwt_vc_json / vc+sd-jwt claim
// shapes, since it operates on the resolved claim object.
//
// A DCQL query looks like:
//   { credentials: [ { id, format, meta: { type_values | vct_values }, claims: [ { path: [...] } ] } ] }
@Injectable()
export class DcqlService {
  // Returns { satisfied, matched: { [credentialQueryId]: disclosedClaims } }.
  //
  // `rejectUnrequestedDisclosures` refuses a presentation that reveals more than
  // the query asked for, rather than quietly dropping the surplus. See the block
  // marked OVER-DISCLOSURE below for why that distinction matters.
  evaluate(
    query: any,
    presented: Array<{
      types: string[];
      vct?: string;
      docType?: string;
      format: string;
      claims: Record<string, any>;
      disclosedNames?: string[];
    }>,
    options: { rejectUnrequestedDisclosures?: boolean } = {},
  ): { satisfied: boolean; matched: Record<string, any>; reason?: string } {
    const credentialQueries = query?.credentials || [];
    if (!Array.isArray(credentialQueries) || credentialQueries.length === 0) {
      return { satisfied: false, matched: {}, reason: 'empty DCQL query' };
    }
    const matched: Record<string, any> = {};
    // Tracks which `presented[]` entries have already satisfied a query, so a
    // single presented credential can't be counted twice against two
    // different credential queries in the same DCQL request.
    const consumed = new Set<number>();
    for (const cq of credentialQueries) {
      const candidateIdx = presented.findIndex(
        (p, idx) => !consumed.has(idx) && this.matchesMeta(cq, p),
      );
      if (candidateIdx === -1) {
        return { satisfied: false, matched, reason: `no credential matched query ${cq.id}` };
      }
      consumed.add(candidateIdx);
      const candidate = presented[candidateIdx];
      const requestedClaims = cq.claims || [];
      const disclosed: Record<string, any> = {};
      for (const claimQuery of requestedClaims) {
        let path: string[] = claimQuery.path || [];
        // Per OID4VP DCQL, W3C VC-format (jwt_vc_json/ldp_vc) claim paths are
        // relative to the full credential and conventionally start with
        // "credentialSubject" (real wallets send/expect this — their DCQL
        // matchers resolve paths against the untouched VC JSON, so a bare
        // `["name"]` path never matches while `["credentialSubject","name"]`
        // does). `candidate.claims` here is already the pre-unwrapped
        // credentialSubject object (see extractCredentials() in
        // oid4vp.service.ts), so strip that leading segment before
        // resolving — bare paths still work for callers that never included
        // the prefix.
        if (
          (candidate.format === 'jwt_vc_json' || candidate.format === 'ldp_vc') &&
          path[0] === 'credentialSubject'
        ) {
          path = path.slice(1);
        }
        const value = this.resolvePath(candidate.claims, path);
        if (value === undefined) {
          return {
            satisfied: false,
            matched,
            reason: `claim ${path.join('.')} missing for query ${cq.id}`,
          };
        }
        if (Array.isArray(claimQuery.values) && !claimQuery.values.includes(value)) {
          return {
            satisfied: false,
            matched,
            reason: `claim ${path.join('.')} value not in allowed set`,
          };
        }
        disclosed[path.join('.')] = value;
      }
      // OVER-DISCLOSURE.
      //
      // Building `disclosed` from the requested paths alone means a holder who
      // reveals more than was asked for is answered normally, with the surplus
      // silently discarded. The relying party never sees it and no decision can
      // turn on it — but the values did leave the wallet and did reach this
      // service, so "the verifier never receives it" was true of the relying
      // party and not of the protocol boundary. Refusing here makes the
      // guarantee the one that was claimed.
      //
      // Only for selective-disclosure formats, and only when the query named
      // claims: a query with no `claims` is asking for the whole credential, so
      // nothing a holder sends can exceed it.
      if (options.rejectUnrequestedDisclosures && requestedClaims.length && candidate.disclosedNames) {
        // Compare on the FIRST path segment. A nested claim is disclosed as its
        // top-level object, so a request for ["address","city"] is satisfied by
        // disclosing `address` — matching the full dotted path would refuse a
        // correct presentation.
        const asked = new Set(
          requestedClaims
            .map((c: any) => (Array.isArray(c.path) ? c.path : []))
            .map((path: string[]) =>
              (candidate.format === 'jwt_vc_json' || candidate.format === 'ldp_vc') &&
              path[0] === 'credentialSubject'
                ? path[1]
                : path[0],
            )
            .filter(Boolean),
        );
        const surplus = candidate.disclosedNames.filter((name) => !asked.has(name));
        if (surplus.length) {
          return {
            satisfied: false,
            matched,
            // Names the claims, not their values: this reason is reported to the
            // relying party, and echoing a value the holder should not have sent
            // would disclose it after refusing to accept it.
            reason:
              `credential for query ${cq.id} disclosed ${surplus.length} claim(s) the request ` +
              `did not ask for: ${surplus.sort().join(', ')}`,
          };
        }
      }
      // No specific claims requested → disclose all.
      matched[cq.id || candidate.types.join('_')] =
        requestedClaims.length ? disclosed : candidate.claims;
    }
    return { satisfied: true, matched };
  }

  private matchesMeta(
    cq: any,
    p: { types: string[]; vct?: string; docType?: string; format: string },
  ): boolean {
    if (cq.format && !sameFormat(cq.format, p.format)) return false;
    const meta = cq.meta || {};
    if (Array.isArray(meta.type_values)) {
      // type_values is an array of allowed type-arrays (OR of AND-sets).
      const ok = meta.type_values.some((set: string[]) =>
        set.every((t) => p.types.includes(t)),
      );
      if (!ok) return false;
    }
    if (Array.isArray(meta.vct_values)) {
      if (!p.vct || !meta.vct_values.includes(p.vct)) return false;
    }
    // mso_mdoc: a single docType string, not an array — one mdoc has exactly
    // one docType, unlike type_values' OR-of-AND-sets shape.
    if (meta.doctype_value) {
      if (!p.docType || p.docType !== meta.doctype_value) return false;
    }
    return true;
  }

  private resolvePath(obj: any, path: string[]): any {
    let cur = obj;
    for (const seg of path) {
      if (cur == null) return undefined;
      cur = cur[seg];
    }
    return cur;
  }
}
