import { Module } from '@nestjs/common';
import { PrismaService } from '../prisma.service';
import { SchemaController } from './schema.controller';
import { SchemaService } from './schema.service';
import { HttpModule } from '@nestjs/axios';
import { UtilsService } from '../utils/utils.service';

@Module({
  imports: [HttpModule],
  controllers: [SchemaController],
  providers: [SchemaService, PrismaService, UtilsService],
})
export class SchemaModule {}
