import { VCModelSchema } from '../entities/VCModelSchema.entity';

export class CreateCredentialDTO {
  schema: VCModelSchema;
  tags: string[];
  status: any;
  deprecatedId?: string;
}
