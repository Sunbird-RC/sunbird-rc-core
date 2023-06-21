import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { VCSModelSchemaInterface } from 'src/types/VCModelSchema.interface';
import { VCSchema } from './VCSchema.entity';

export class VCModelSchema implements VCSModelSchemaInterface {
  @ApiProperty({ type: String, description: 'id' })
  type: string;
  @ApiProperty({ type: String, description: 'version' })
  version: string;
  @ApiProperty({ type: String, description: 'id' })
  id: string;
  @ApiProperty({ type: String, description: 'name' })
  name: string;
  @ApiProperty({ type: String, description: 'author' })
  author: string;
  @ApiProperty({ type: String, description: 'authored' })
  authored: string;
  @ApiProperty({ type: VCSchema, description: 'schema' })
  schema: {
    $id: string;
    $schema: string;
    description: string;
    name?: string;
    type: string;
    properties: {
      [k: string]: unknown;
    };
    required: [] | [string];
    additionalProperties: boolean;
    [k: string]: unknown;
  };
  @ApiPropertyOptional({
    type: JSON,
  })
  proof?: {
    [k: string]: unknown;
  };
  [k: string]: unknown;
}
