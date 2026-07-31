// The credential-schema backend enforces NO status-transition validation at
// all — any status can jump to any other, and an unrecognized string
// silently becomes DRAFT. This state machine is enforced client-side only,
// so the UI behaves sanely even though the backend won't stop a bad request.
export type SchemaStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED' | 'REVOKED'

export const SCHEMA_TRANSITIONS: Record<SchemaStatus, SchemaStatus[]> = {
  DRAFT: ['PUBLISHED', 'REVOKED'],
  PUBLISHED: ['DEPRECATED', 'REVOKED'],
  DEPRECATED: ['REVOKED'],
  REVOKED: [],
}

export function allowedTransitions(status: SchemaStatus): SchemaStatus[] {
  return SCHEMA_TRANSITIONS[status] ?? []
}

export function isTerminal(status: SchemaStatus): boolean {
  return allowedTransitions(status).length === 0
}
