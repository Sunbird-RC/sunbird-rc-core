import { ApiProperty } from '@nestjs/swagger';

ApiProperty;
export class TemplateBody {
  @ApiProperty({ type: String, description: 'Schema ID' })
  schemaId: string;
  @ApiProperty({ type: String, description: 'Template' })
  template: string;
  @ApiProperty({ type: String, description: 'Type of Template' })
  type: string;
}
