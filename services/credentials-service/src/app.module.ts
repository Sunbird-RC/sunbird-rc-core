import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaService } from './prisma.service';
import { CredentialsModule } from './credentials/credentials.module';
// import { PresentationsModule } from './presentations/presentations.module';
// import { ExchangesModule } from './exchanges/exchanges.module';

@Module({
  imports: [
    HttpModule,
    ConfigModule.forRoot({ isGlobal: true }),
    // IssuingModule,
    // VerifyingModule,
    CredentialsModule,
    // PresentationsModule,
    // ExchangesModule,
  ],
  controllers: [AppController],
  providers: [AppService, ConfigService, PrismaService, /*IssuingService*/],
})
export class AppModule {}
