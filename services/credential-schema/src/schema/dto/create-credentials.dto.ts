import { VCModelSchema } from '../entities/VCModelSchema.entity';

export interface Oid4vciConfig {
  oid4vciEnabled: boolean;
  oid4vciFormats?: string[]; // e.g. ['ldp_vc', 'jwt_vc_json', 'vc+sd-jwt']
  vct?: string;
  display?: Record<string, any>[];
}

export class CreateCredentialDTO {
  schema: VCModelSchema;
  tags: string[];
  status: any;
  deprecatedId?: string;
  oid4vciConfig?: Oid4vciConfig; // optional OID4VCI opt-in
}
