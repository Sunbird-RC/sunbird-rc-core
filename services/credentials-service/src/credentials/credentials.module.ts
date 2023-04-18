import { Module } from '@nestjs/common';
import { CredentialsService } from './credentials.service';
import { CredentialsController } from './credentials.controller';
import { HttpModule } from '@nestjs/axios';
import { PrismaService } from '../prisma.service';

@Module({
  imports: [HttpModule],
  providers: [CredentialsService, PrismaService],
  controllers: [CredentialsController],
})
export class CredentialsModule {}
