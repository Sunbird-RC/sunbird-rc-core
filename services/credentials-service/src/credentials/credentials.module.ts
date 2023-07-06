import { Module } from '@nestjs/common';
import { CredentialsService } from './credentials.service';
import { CredentialsController } from './credentials.controller';
import { HttpModule } from '@nestjs/axios';
import { PrismaService } from '../prisma.service';
import { IdentityUtilsService } from './utils/identity.utils.service';

@Module({
  imports: [HttpModule],
  providers: [CredentialsService, PrismaService, IdentityUtilsService],
  controllers: [CredentialsController],
})
export class CredentialsModule {}
