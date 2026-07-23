import { Injectable } from '@nestjs/common';

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
  evaluate(
    query: any,
    presented: Array<{ types: string[]; vct?: string; format: string; claims: Record<string, any> }>,
  ): { satisfied: boolean; matched: Record<string, any>; reason?: string } {
    const credentialQueries = query?.credentials || [];
    if (!Array.isArray(credentialQueries) || credentialQueries.length === 0) {
      return { satisfied: false, matched: {}, reason: 'empty DCQL query' };
    }
    const matched: Record<string, any> = {};
    for (const cq of credentialQueries) {
      const candidate = presented.find((p) => this.matchesMeta(cq, p));
      if (!candidate) {
        return { satisfied: false, matched, reason: `no credential matched query ${cq.id}` };
      }
      const requestedClaims = cq.claims || [];
      const disclosed: Record<string, any> = {};
      for (const claimQuery of requestedClaims) {
        let path: string[] = claimQuery.path || [];
        // Per OID4VP DCQL, W3C VC-format (jwt_vc_json/ldp_vc) claim paths are
        // relative to the full credential and conventionally start with
        // "credentialSubject" (real wallets, e.g. walt.id, send/expect this —
        // found live: walt.id's own DCQL matcher resolves paths against the
        // untouched VC JSON, so a bare `["name"]` path never matches while
        // `["credentialSubject","name"]` does). `candidate.claims` here is
        // already the pre-unwrapped credentialSubject object (see
        // extractCredentials() in oid4vp.service.ts), so strip that leading
        // segment before resolving — bare paths still work for callers that
        // never included the prefix.
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
      // No specific claims requested → disclose all.
      matched[cq.id || candidate.types.join('_')] =
        requestedClaims.length ? disclosed : candidate.claims;
    }
    return { satisfied: true, matched };
  }

  private matchesMeta(
    cq: any,
    p: { types: string[]; vct?: string; format: string },
  ): boolean {
    if (cq.format && cq.format !== p.format) return false;
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
