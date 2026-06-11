import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { PrismaService } from 'src/utils/prisma.service';
import { DidController } from './did.controller';
import { DidService } from './did.service';
import { SecretsModule } from '../secrets/secrets.module';

@Module({
  imports: [HttpModule, SecretsModule],
  controllers: [DidController],
  providers: [
    DidService,
    PrismaService,
  ],
})
export class DidModule {}
