import { Module } from '@nestjs/common';
import { CredentialsService } from './credentials.service';
import { CredentialsController } from './credentials.controller';
import { HttpModule } from '@nestjs/axios';
import { IdentityUtilsService } from './utils/identity.utils.service';
import { RenderingUtilsService } from './utils/rendering.utils.service';
import { SchemaUtilsSerivce } from './utils/schema.utils.service';
import { CredentialFormatService } from './utils/credential-format.service';
import { StatusListService } from './utils/status-list.service';
import { RevocationListImpl } from '../revocation-list/revocation-list.impl';
import { PrismaClient } from '@prisma/client';

@Module({
  imports: [HttpModule],
  providers: [
    CredentialsService,
    PrismaClient,
    IdentityUtilsService,
    RenderingUtilsService,
    SchemaUtilsSerivce,
    CredentialFormatService,
    StatusListService,
    RevocationListImpl,
  ],
  controllers: [CredentialsController],
  exports: [IdentityUtilsService],
})
export class CredentialsModule {}
