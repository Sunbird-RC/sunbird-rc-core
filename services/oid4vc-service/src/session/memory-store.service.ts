import { Injectable, Logger } from '@nestjs/common';
import { SessionStore } from './session-store.interface';

interface Entry {
  value: any;
  expiresAt: number;
}

// In-memory session store. Suitable for single-instance and test deployments.
// NOT safe across multiple replicas — use Redis in production.
@Injectable()
export class MemoryStoreService implements SessionStore {
  private readonly logger = new Logger(MemoryStoreService.name);
  private readonly map = new Map<string, Entry>();

  constructor() {
    this.logger.warn(
      'Using in-memory session store — do NOT run multiple replicas with this backend. Set SESSION_STORE=redis for production.',
    );
    // Lazy sweep of expired entries.
    setInterval(() => this.sweep(), 60_000).unref?.();
  }

  async set(key: string, value: any, ttlSeconds: number): Promise<void> {
    this.map.set(key, { value, expiresAt: Date.now() + ttlSeconds * 1000 });
  }

  async get<T = any>(key: string): Promise<T | null> {
    const entry = this.map.get(key);
    if (!entry) return null;
    if (entry.expiresAt < Date.now()) {
      this.map.delete(key);
      return null;
    }
    return entry.value as T;
  }

  async getdel<T = any>(key: string): Promise<T | null> {
    // Deliberately not `await this.get(key)` then delete: an `await` yields
    // to the microtask queue between the two, so two callers racing on the
    // same single-use key (a pre-authorized code, a c_nonce) could both
    // observe the entry before either deletes it — defeating the single-use
    // guarantee this method exists for. Read and delete here with no `await`
    // between them, so the pair runs as one uninterruptible synchronous step.
    const entry = this.map.get(key);
    if (!entry) return null;
    this.map.delete(key);
    if (entry.expiresAt < Date.now()) return null;
    return entry.value as T;
  }

  async del(key: string): Promise<void> {
    this.map.delete(key);
  }

  private sweep() {
    const now = Date.now();
    for (const [k, v] of this.map.entries()) {
      if (v.expiresAt < now) this.map.delete(k);
    }
  }
}
