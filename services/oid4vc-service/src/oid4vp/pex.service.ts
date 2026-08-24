import { Injectable } from '@nestjs/common';
import { sameFormat } from './dcql.service';
import { parseJsonPath, walkSegments } from './jsonpath.util';

type Presented = {
  types: string[];
  vct?: string;
  docType?: string;
  format: string;
  claims: Record<string, any>;
};

type DescriptorResult = { matched: boolean; disclosed?: Record<string, any>; consumedIdx?: number };

// DIF Presentation Exchange v2.0 evaluator. Covers input_descriptors[]
// constraints.fields path+filter matching, format restriction, and
// submission_requirements pick/all groups (including from_nested).
//
// A presentation_definition looks like:
//   { input_descriptors: [ { id, format?, group?, constraints: { fields: [
//       { path: [...alternatives], filter?, optional? } ] } } ],
//     submission_requirements?: [ { rule: 'all'|'pick', from?, from_nested?, count?, min?, max? } ] }
@Injectable()
export class PexService {
  // Returns { satisfied, matched: { [input_descriptor.id]: disclosedClaims } }.
  evaluate(
    presentationDefinition: any,
    presented: Presented[],
  ): { satisfied: boolean; matched: Record<string, any>; reason?: string } {
    const descriptors = presentationDefinition?.input_descriptors;
    if (!Array.isArray(descriptors) || descriptors.length === 0) {
      return { satisfied: false, matched: {}, reason: 'empty presentation_definition' };
    }

    // Attempt every descriptor greedily (first-fit against not-yet-consumed
    // presented entries), regardless of submission_requirements — the
    // requirement tree below decides which of these attempts actually needed
    // to succeed. Non-backtracking: once a presented entry is consumed by a
    // descriptor, no earlier descriptor gets a chance to reclaim it.
    const consumed = new Set<number>();
    const results = new Map<string, DescriptorResult>();
    for (const desc of descriptors) {
      let foundIdx = -1;
      let foundDisclosed: Record<string, any> | undefined;
      for (let i = 0; i < presented.length; i++) {
        if (consumed.has(i)) continue;
        const disclosed = this.matchDescriptor(desc, presented[i]);
        if (disclosed !== undefined) {
          foundIdx = i;
          foundDisclosed = disclosed;
          break;
        }
      }
      if (foundIdx === -1) {
        results.set(desc.id, { matched: false });
        continue;
      }
      consumed.add(foundIdx);
      results.set(desc.id, { matched: true, disclosed: foundDisclosed, consumedIdx: foundIdx });
    }

    const requirements = presentationDefinition?.submission_requirements;
    if (!Array.isArray(requirements) || requirements.length === 0) {
      // No submission_requirements → every descriptor is mandatory (DCQL parity).
      for (const desc of descriptors) {
        if (!results.get(desc.id)?.matched) {
          return { satisfied: false, matched: {}, reason: `no credential matched input_descriptor ${desc.id}` };
        }
      }
      const matched: Record<string, any> = {};
      for (const desc of descriptors) matched[desc.id] = results.get(desc.id)!.disclosed;
      return { satisfied: true, matched };
    }

    // submission_requirements present: top-level entries are ANDed together
    // (DIF PEX v2.0 §submission_requirements).
    const groupOf = new Map<string, string[]>(); // group tag -> descriptor ids tagged with it
    for (const desc of descriptors) {
      for (const g of desc.group || []) {
        if (!groupOf.has(g)) groupOf.set(g, []);
        groupOf.get(g)!.push(desc.id);
      }
    }

    const usedIds = new Set<string>();
    for (const req of requirements) {
      const result = this.evaluateRequirement(req, results, groupOf);
      if (!result.ok) {
        return {
          satisfied: false,
          matched: {},
          reason: `submission_requirements not satisfied: ${req.name || req.from || 'group'}`,
        };
      }
      result.usedIds.forEach((id) => usedIds.add(id));
    }

    const matched: Record<string, any> = {};
    for (const id of usedIds) matched[id] = results.get(id)?.disclosed;
    return { satisfied: true, matched };
  }

  private evaluateRequirement(
    req: any,
    results: Map<string, DescriptorResult>,
    groupOf: Map<string, string[]>,
  ): { ok: boolean; usedIds: string[] } {
    if (req.from) {
      const ids = groupOf.get(req.from) || [];
      const satisfiedIds = ids.filter((id) => results.get(id)?.matched);
      const ok = this.countSatisfiesRule(req, satisfiedIds.length, ids.length);
      return { ok, usedIds: ok ? satisfiedIds : [] };
    }
    if (Array.isArray(req.from_nested)) {
      const nestedResults = req.from_nested.map((r: any) => this.evaluateRequirement(r, results, groupOf));
      const passingCount = nestedResults.filter((r: { ok: boolean }) => r.ok).length;
      const ok = this.countSatisfiesRule(req, passingCount, nestedResults.length);
      const usedIds = ok
        ? nestedResults.filter((r: { ok: boolean }) => r.ok).flatMap((r: { usedIds: string[] }) => r.usedIds)
        : [];
      return { ok, usedIds };
    }
    return { ok: false, usedIds: [] };
  }

  private countSatisfiesRule(req: any, n: number, total: number): boolean {
    if (req.rule === 'all') return n === total;
    // rule === 'pick'
    const count = req.count;
    const min = req.min ?? (count !== undefined ? count : 1);
    const max = req.max ?? (count !== undefined ? count : total);
    return n >= min && n <= max;
  }

  // Returns the disclosed-claims map if `desc` matches `p`, else undefined.
  private matchDescriptor(desc: any, p: Presented): Record<string, any> | undefined {
    const formatKeys = desc.format ? Object.keys(desc.format) : [];
    if (formatKeys.length && !formatKeys.some((f) => sameFormat(f, p.format))) return undefined;

    const fields = desc.constraints?.fields || [];
    if (!fields.length) return p.claims;

    const disclosed: Record<string, any> = {};
    for (const field of fields) {
      const resolved = this.resolveField(field, p);
      if (resolved === undefined) {
        if (field.optional) continue;
        return undefined;
      }
      if (!this.matchesFilter(field.filter, resolved.value)) return undefined;
      disclosed[resolved.key] = resolved.value;
    }
    return disclosed;
  }

  private resolveField(field: any, p: Presented): { key: string; value: any } | undefined {
    const paths: string[] = field.path || [];
    for (const path of paths) {
      const segments = this.stripWrapperSegments(path, p.format);
      const value = walkSegments(p.claims, segments);
      if (value !== undefined) return { key: segments.join('.'), value };
    }
    return undefined;
  }

  // p.claims is already the pre-unwrapped credentialSubject object (see
  // extractCredentials() in oid4vp.service.ts), but a field path is written
  // against the full credential shape — strip a leading vc/credentialSubject
  // segment for W3C VC formats before resolving against p.claims.
  private stripWrapperSegments(path: string, format: string): Array<string | number> {
    let segments = parseJsonPath(path);
    if (format === 'jwt_vc_json' || format === 'ldp_vc') {
      if (segments[0] === 'vc') segments = segments.slice(1);
      if (segments[0] === 'credentialSubject') segments = segments.slice(1);
    }
    return segments;
  }

  // filter subset: const (equality), enum (membership), pattern (regex).
  // Unknown keyword → fails closed (treated as non-matching), not silently
  // ignored.
  // ponytail: const/enum/pattern only — add via a real JSON-schema validator
  // (ajv) if a producer's definition ever needs allOf/minimum/format/etc.
  private matchesFilter(filter: any, value: any): boolean {
    if (!filter) return true;
    const keys = Object.keys(filter);
    if (keys.some((k) => !['const', 'enum', 'pattern'].includes(k))) return false;
    if ('const' in filter && value !== filter.const) return false;
    if ('enum' in filter && !filter.enum.includes(value)) return false;
    if ('pattern' in filter && !new RegExp(filter.pattern).test(String(value))) return false;
    return true;
  }
}