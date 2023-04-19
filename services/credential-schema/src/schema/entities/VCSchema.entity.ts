import { ApiProperty } from '@nestjs/swagger';

export class VCSchema {
  @ApiProperty({ type: String, description: 'id' })
  $id: string;
  @ApiProperty({ type: String, description: 'version' })
  $schema: string;
  @ApiProperty({ type: String, description: 'id' })
  description: string;
  @ApiProperty({ type: String, description: 'name' })
  name?: string;
  @ApiProperty({ type: String, description: 'author' })
  type: string;
  @ApiProperty({
    type: JSON,
    description: 'properties that define a particular schema',
  })
  properties: {
    [k: string]: unknown;
  };
  @ApiProperty({ type: [String], description: 'required properties' })
  required: [] | [string];
  @ApiProperty({
    type: Boolean,
    description: 'if the schema contains some additional properties',
  })
  additionalProperties: boolean;
  [k: string]: unknown;
}
