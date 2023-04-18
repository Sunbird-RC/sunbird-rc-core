import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaService } from './prisma.service';
import { DidService } from './did/did.service';
import { DidController } from './did/did.controller';
import { DidModule } from './did/did.module';
import { HttpModule } from '@nestjs/axios';
import { KycModule } from './kyc/kyc.module';
import { ConfigModule } from '@nestjs/config';
import { VcModule } from './vc/vc.module';
import { VaultService } from './did/vault.service';

@Module({
  imports: [
    DidModule,
    KycModule,
    VcModule,
    HttpModule,
    ConfigModule.forRoot({
      isGlobal: true,
    }),
  ],
  controllers: [AppController, DidController],
  providers: [AppService, PrismaService, DidService, VaultService],
})
export class AppModule {}
