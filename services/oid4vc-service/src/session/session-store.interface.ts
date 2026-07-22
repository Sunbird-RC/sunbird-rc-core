// Abstract session store for short-lived OID4VC protocol state.
// Two implementations: in-memory (single-instance/test) and Redis (production).
// The `getdel` primitive gives atomic single-use semantics for pre-auth codes
// and nonces — critical for replay protection.
export const SESSION_STORE = Symbol('SESSION_STORE');

export interface SessionStore {
  // Store a JSON-serialisable value with a TTL in seconds.
  set(key: string, value: any, ttlSeconds: number): Promise<void>;
  // Read a value (no consume). Returns null if missing/expired.
  get<T = any>(key: string): Promise<T | null>;
  // Atomically read-and-delete. Returns null if missing/expired.
  // Used for single-use tokens (pre-auth code, c_nonce).
  getdel<T = any>(key: string): Promise<T | null>;
  // Delete a key.
  del(key: string): Promise<void>;
}
