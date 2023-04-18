import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { DidModule } from 'src/did/did.module';
import { DidService } from 'src/did/did.service';
import { VaultService } from 'src/did/vault.service';
import { PrismaService } from 'src/prisma.service';
import { KycController } from './kyc.controller';
import KycService from './kyc.service';

@Module({
  imports: [HttpModule,],
  controllers: [KycController],
  providers: [KycService, PrismaService, DidService, VaultService],
})
export class KycModule {}
