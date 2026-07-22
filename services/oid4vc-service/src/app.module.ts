import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { AppController } from './app.controller';
import { SessionModule } from './session/session.module';
import { ClientsModule } from './clients/clients.module';
import { Oid4vciModule } from './oid4vci/oid4vci.module';
import { Oid4vpModule } from './oid4vp/oid4vp.module';
import { loadConfig } from './config/configuration';

// OID4VP is only mounted when enabled (default on). OID4VCI is always on.
const optionalModules = loadConfig().oid4vpEnabled ? [Oid4vpModule] : [];

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    SessionModule,
    ClientsModule,
    Oid4vciModule,
    ...optionalModules,
  ],
  controllers: [AppController],
})
export class AppModule {}
