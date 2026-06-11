import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { PrismaService } from './utils/prisma.service';
import { DidService } from './did/did.service';
import { DidController } from './did/did.controller';
import { DidModule } from './did/did.module';
import { HttpModule } from '@nestjs/axios';
import { ConfigModule } from '@nestjs/config';
import { VcModule } from './vc/vc.module';
import { APP_GUARD } from '@nestjs/core';
import { AuthGuard } from './auth/auth.guard';
import { TerminusModule } from '@nestjs/terminus';
import { PrismaHealthIndicator } from './utils/prisma.health';
import { SecretsHealthIndicator } from './utils/secrets.health';
import { SecretsModule } from './secrets/secrets.module';

@Module({
  imports: [
    SecretsModule,
    DidModule,
    VcModule,
    HttpModule,
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    TerminusModule
  ],
  controllers: [AppController, DidController],
  providers: [
    PrismaService, DidService,
    {
      provide: APP_GUARD,
      useClass: AuthGuard,
    },
    PrismaHealthIndicator,
    SecretsHealthIndicator,
  ],
})
export class AppModule {}
