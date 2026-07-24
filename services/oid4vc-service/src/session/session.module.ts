import { Global, Module } from '@nestjs/common';
import { SESSION_STORE } from './session-store.interface';
import { MemoryStoreService } from './memory-store.service';
import { RedisStoreService } from './redis-store.service';
import { loadConfig } from '../config/configuration';

// Picks the session-store implementation from config at boot. Exposed globally
// so every feature module injects the same store via the SESSION_STORE token.
@Global()
@Module({
  providers: [
    MemoryStoreService,
    RedisStoreService,
    {
      provide: SESSION_STORE,
      useFactory: (memory: MemoryStoreService, redis: RedisStoreService) =>
        loadConfig().sessionStore === 'redis' ? redis : memory,
      inject: [MemoryStoreService, RedisStoreService],
    },
  ],
  exports: [SESSION_STORE],
})
export class SessionModule {}
