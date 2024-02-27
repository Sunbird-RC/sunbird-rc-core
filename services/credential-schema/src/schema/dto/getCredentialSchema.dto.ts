import { VCSchema } from '../entities/VCSchema.entity';

export class GetCredentialSchemaDTO {
  schema: {
    type: string;
    id: string;
    version: string;
    name: string;
    author: string;
    authored: Date;
    schema: any; //VCSchema
    // proof?: {
    //   [k: string]: unknown;
    // };
    proof: any;
  };
  tags: string[];
  status: string;
  createdAt: Date;
  updatedAt: Date;
  createdBy: string;
  updatedBy: string;
  deprecatedId: string;
}
