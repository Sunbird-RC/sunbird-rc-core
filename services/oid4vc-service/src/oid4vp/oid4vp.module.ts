import { Module } from '@nestjs/common';
import { Oid4vpController } from './oid4vp.controller';
import { Oid4vpService } from './oid4vp.service';
import { DcqlService } from './dcql.service';
import { Oid4vciModule } from '../oid4vci/oid4vci.module';

// Imports Oid4vciModule to reuse TokenService (issuer DID + JAR signing).
@Module({
  imports: [Oid4vciModule],
  controllers: [Oid4vpController],
  providers: [Oid4vpService, DcqlService],
})
export class Oid4vpModule {}
