import { Module } from '@nestjs/common';
import { PrismaService } from '../prisma.service';
import { SchemaController } from './schema.controller';
import { SchemaService } from './schema.service';

@Module({
  controllers: [SchemaController],
  providers: [SchemaService, PrismaService],
})
export class SchemaModule {}
