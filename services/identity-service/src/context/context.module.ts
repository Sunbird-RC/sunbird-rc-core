import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { PrismaService } from 'src/utils/prisma.service';
import { ContextController } from './context.controller';
import { ContextService } from './context.service';

@Module({
  imports: [HttpModule],
  controllers: [ContextController],
  providers: [
    ContextService,
    PrismaService
  ],
})
export class ContextModule {}
