import { Module } from '@nestjs/common';
import { RevocationListService } from './revocation-list.service';
import { RevocationList } from './revocation-list.helper';
import { PrismaClient } from '@prisma/client';
import { RevocationListImpl } from './revocation-list.impl';
import { CredentialsModule } from 'src/credentials/credentials.module';
import { IdentityUtilsService } from 'src/credentials/utils/identity.utils.service';
import { HttpModule, HttpService } from '@nestjs/axios';

@Module({
  imports: [HttpModule],
  providers: [RevocationList, RevocationListService, PrismaClient, RevocationListImpl, IdentityUtilsService]
})
export class RevocationListModule {}
