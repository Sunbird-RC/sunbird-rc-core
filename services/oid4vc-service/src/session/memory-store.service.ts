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
    const val = await this.get<T>(key);
    this.map.delete(key);
    return val;
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
