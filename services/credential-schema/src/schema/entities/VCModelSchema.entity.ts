import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { VCSModelSchemaInterface } from 'src/types/VCModelSchema.interface';
import { VCSchema } from './VCSchema.entity';
import { Status as PrismaStatus} from '@prisma/client'

// export class VCModelSchema implements VCSModelSchemaInterface {
//   @ApiProperty({ type: String, description: 'type' })
//   type: string;
//   @ApiProperty({ type: String, description: 'version' })
//   version: string;
//   @ApiProperty({ type: String, description: 'id' })
//   id: string;
//   @ApiProperty({ type: String, description: 'name' })
//   name: string;
//   @ApiProperty({ type: String, description: 'author' })
//   author: string;
//   @ApiProperty({ type: String, description: 'authored' })
//   authored: string;
//   @ApiProperty({ type: VCSchema, description: 'schema' })
//   schema: {
//     $id: string;
//     $schema: string;
//     description: string;
//     name?: string;
//     type: string;
//     properties: {
//       [k: string]: unknown;
//     };
//     required: [] | [string];
//     additionalProperties: boolean;
//     [k: string]: unknown;
//   };
//   @ApiProperty({ type: Array<String>, description: 'tags for the schema' })
//   tags: string[];
//   @ApiProperty({ enum: PrismaStatus, description: 'current schema staus' })
//   status: PrismaStatus
//   @ApiPropertyOptional({
//     type: JSON,
//   })
//   proof?: {
//     [k: string]: unknown;
//   };
//   [k: string]: unknown;
// }

export class VCModelSchema implements VCSModelSchemaInterface {
  @ApiProperty({ type: Object, description: 'Schema payload'})
  schema: {
    type: string;
    version: string;
    id: string;
    name: string;
    author: string;
    authored: string;
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
  };

  @ApiProperty({ type: Array<String>, description: 'tags for the schema' })
  tags: [] | [string];
  @ApiProperty({ enum: PrismaStatus, description: 'current schema staus' })
  status: PrismaStatus
}
