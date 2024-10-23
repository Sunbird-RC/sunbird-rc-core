import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { DidService } from 'src/did/did.service';
import { VaultService } from 'src/utils/vault.service';
import { PrismaService } from 'src/utils/prisma.service';
import { VcController } from './vc.controller';
import VcService from './vc.service';
import { AnchorCordService } from 'src/utils/cord.service';

@Module({
  imports: [HttpModule],
  controllers: [VcController],
  providers: [VcService, PrismaService, DidService, VaultService,AnchorCordService],
})
export class VcModule {}
