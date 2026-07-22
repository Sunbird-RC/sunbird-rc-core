import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { DidService } from 'src/did/did.service';
import { VaultService } from 'src/utils/vault.service';
import { PrismaService } from 'src/utils/prisma.service';
import { VcController } from './vc.controller';
import VcService from './vc.service';
import { JwtSignerService } from './jwt.service';
import { WellKnownController } from '../well-known/well-known.controller';

@Module({
  imports: [HttpModule],
  controllers: [VcController, WellKnownController],
  providers: [VcService, JwtSignerService, PrismaService, DidService, VaultService],
  exports: [JwtSignerService],
})
export class VcModule {}
