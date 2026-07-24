import { Module } from '@nestjs/common';
import { Oid4vciController } from './oid4vci.controller';
import { MetadataController } from './metadata.controller';
import { Oid4vciService } from './oid4vci.service';
import { TokenService } from './token.service';
import { PopService } from './pop.service';

@Module({
  controllers: [Oid4vciController, MetadataController],
  providers: [Oid4vciService, TokenService, PopService],
  exports: [Oid4vciService, TokenService],
})
export class Oid4vciModule {}
