import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

// represents the Template model created in Prisma
export class Template {
  @ApiProperty({ type: String, description: 'Unique CUID' })
  templateId: string;
  @ApiProperty({ type: String, description: 'Schema ID' })
  schemaId: string;
  @ApiProperty({ type: String, description: 'HTML template' })
  template: string;
  @ApiProperty({ type: String, description: 'type' })
  type: string;
  @ApiProperty({ type: Date, description: 'createdAt' })
  createdAt: Date;
  @ApiPropertyOptional({ type: Date, description: 'updatedAt' })
  updatedAt: Date;
  @ApiPropertyOptional({ type: Date, description: 'createdBy' })
  createdBy: Date;
  @ApiPropertyOptional({ type: Date, description: 'updatedBy' })
  updatedBy: Date;
}
