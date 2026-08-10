import { Injectable, Logger } from '@nestjs/common';
import Redis from 'ioredis';
import { SessionStore } from './session-store.interface';
import { loadConfig } from '../config/configuration';

// Redis-backed session store. Uses native TTL and GETDEL for atomic single-use
// semantics. All keys are namespaced by the callers (oid4vc: / oid4vp:).
@Injectable()
export class RedisStoreService implements SessionStore {
  private readonly logger = new Logger(RedisStoreService.name);
  private readonly redis: Redis;

  constructor() {
    const { redisUrl } = loadConfig();
    // lazyConnect so this provider is harmless when SESSION_STORE=memory:
    // it only dials Redis on the first command, which never happens in memory mode.
    this.redis = new Redis(redisUrl, { lazyConnect: true, maxRetriesPerRequest: 2 });
    this.redis.on('error', (err) => this.logger.error(`Redis error: ${err}`));
  }

  async set(key: string, value: any, ttlSeconds: number): Promise<void> {
    await this.redis.set(key, JSON.stringify(value), 'EX', ttlSeconds);
  }

  async get<T = any>(key: string): Promise<T | null> {
    const raw = await this.redis.get(key);
    return raw ? (JSON.parse(raw) as T) : null;
  }

  async getdel<T = any>(key: string): Promise<T | null> {
    // GETDEL requires Redis >= 6.2; fall back to MULTI for older servers.
    let raw: string | null;
    try {
      raw = await (this.redis as any).getdel(key);
    } catch {
      const [[, val]] = (await this.redis
        .multi()
        .get(key)
        .del(key)
        .exec()) as any;
      raw = val;
    }
    return raw ? (JSON.parse(raw) as T) : null;
  }

  async del(key: string): Promise<void> {
    await this.redis.del(key);
  }
}
