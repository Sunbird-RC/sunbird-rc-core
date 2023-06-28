import { Module } from '@nestjs/common';
import { PrismaService } from 'src/prisma.service';
import { SchemaController } from './schema.controller';
import { SchemaService } from './schema.service';
import { HttpModule, HttpService } from '@nestjs/axios';

@Module({
  imports: [HttpModule],
  controllers: [SchemaController],
  providers: [SchemaService, PrismaService],
})
export class SchemaModule {}
