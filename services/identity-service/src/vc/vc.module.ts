import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { DidService } from 'src/did/did.service';
import { SecretsModule } from 'src/secrets/secrets.module';
import { PrismaService } from 'src/utils/prisma.service';
import { VcController } from './vc.controller';
import VcService from './vc.service';

@Module({
  imports: [HttpModule, SecretsModule],
  controllers: [VcController],
  providers: [VcService, PrismaService, DidService],
})
export class VcModule {}
