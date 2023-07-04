import { Module } from '@nestjs/common';
import { SchemaController } from './schema.controller';
import { SchemaService } from './schema.service';
import { HttpModule } from '@nestjs/axios';
import { UtilsService } from '../utils/utils.service';
import { PrismaClient } from '@prisma/client';

@Module({
  imports: [HttpModule],
  controllers: [SchemaController],
  providers: [SchemaService, PrismaClient, UtilsService],
})
export class SchemaModule {}
