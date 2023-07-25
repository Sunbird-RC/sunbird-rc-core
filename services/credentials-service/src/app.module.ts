import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { CredentialsModule } from './credentials/credentials.module';
import { TerminusModule } from '@nestjs/terminus';
import { HealthCheckUtilsService } from './credentials/utils/healthcheck.utils.service';
import { PrismaClient } from '@prisma/client';

@Module({
  imports: [
    HttpModule,
    ConfigModule.forRoot({ isGlobal: true }),
    CredentialsModule,
    TerminusModule
  ],
  controllers: [AppController],
  providers: [AppService, ConfigService, PrismaClient, HealthCheckUtilsService],
})
export class AppModule {}
