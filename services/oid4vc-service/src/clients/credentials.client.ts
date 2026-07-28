import { HttpService } from '@nestjs/axios';
import { Injectable, InternalServerErrorException, Logger } from '@nestjs/common';
import { loadConfig } from '../config/configuration';

// Thin wrapper over credentials-service. Unchanged external contract; the
// only new thing we pass is the optional `format` field (ldp_vc default).
@Injectable()
export class CredentialsClient {
  private readonly logger = new Logger(CredentialsClient.name);
  private readonly baseUrl = loadConfig().credentialServiceBaseUrl;

  constructor(private readonly http: HttpService) {}

  async issue(payload: {
    credential: any;
    credentialSchemaId: string;
    credentialSchemaVersion: string;
    tags: string[];
    method?: string;
    format?: string;
    disclosable?: string[];
    holderJwk?: Record<string, any>;
    docType?: string;
    namespaces?: Record<string, Record<string, any>>;
    // vc+sd-jwt only: overrides credentials-service's own vct derivation
    // (which defaults to the last `credential.type` entry — a bare display
    // name). Pass the same normalized (URI-form) vct published in issuer
    // metadata — see vct.util.ts — so the issued credential's vct matches
    // what a wallet resolves from metadata / uses in DCQL vct_values.
    vct?: string;
  }): Promise<{ credential: any; format?: string }> {
    try {
      const res = await this.http.axiosRef.post(
        `${this.baseUrl}/credentials/issue`,
        payload,
      );
      return res.data;
    } catch (err) {
      this.logger.error(`Error issuing credential: ${err}`);
      throw new InternalServerErrorException('Error issuing credential');
    }
  }

  async verify(
    verifiableCredential: any,
    options?: { challenge?: string; domain?: string },
  ): Promise<any> {
    try {
      const res = await this.http.axiosRef.post(
        `${this.baseUrl}/credentials/verify`,
        { verifiableCredential, options },
      );
      return res.data;
    } catch (err) {
      this.logger.error(`Error verifying credential: ${err}`);
      return { errors: ['Error verifying credential'] };
    }
  }

  async getStatusList(id: string): Promise<any> {
    const res = await this.http.axiosRef.get(
      `${this.baseUrl}/credentials/status-list/${encodeURIComponent(id)}`,
    );
    return res.data;
  }
}
