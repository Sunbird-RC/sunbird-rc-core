import { HttpService } from '@nestjs/axios';
import { Injectable, InternalServerErrorException, Logger } from '@nestjs/common';
import { loadConfig } from '../config/configuration';

// Thin wrapper over identity-service (DID resolve + JWT sign/verify).
// oid4vc-service delegates all signing here; it holds no credential keys.
@Injectable()
export class IdentityClient {
  private readonly logger = new Logger(IdentityClient.name);
  private readonly baseUrl = loadConfig().identityBaseUrl;

  constructor(private readonly http: HttpService) {}

  async resolveDID(did: string): Promise<any> {
    try {
      const res = await this.http.axiosRef.get(
        `${this.baseUrl}/did/resolve/${encodeURIComponent(did)}`,
      );
      return res.data;
    } catch (err) {
      this.logger.error(`Error resolving DID ${did}: ${err}`);
      throw new InternalServerErrorException('Error resolving DID');
    }
  }

  async generateDID(method = 'web'): Promise<any> {
    const res = await this.http.axiosRef.post(`${this.baseUrl}/did/generate`, {
      content: [
        {
          alsoKnownAs: [],
          services: [{ id: 'oid4vc', type: 'OID4VCService' }],
          method,
        },
      ],
    });
    return res.data?.[0] || res.data;
  }

  async signJwt(
    did: string,
    payload: object,
    header: Record<string, any> = {},
  ): Promise<string> {
    try {
      const res = await this.http.axiosRef.post(`${this.baseUrl}/utils/sign-jwt`, {
        DID: did,
        payload,
        header,
      });
      return res.data?.jwt;
    } catch (err) {
      this.logger.error(`Error signing JWT: ${err}`);
      throw new InternalServerErrorException('Error signing JWT');
    }
  }

  async verifyJwt(
    jwt: string,
    did?: string,
  ): Promise<{ verified: boolean; payload?: any; error?: string }> {
    try {
      const res = await this.http.axiosRef.post(`${this.baseUrl}/utils/verify-jwt`, {
        jwt,
        DID: did,
      });
      return res.data;
    } catch (err) {
      this.logger.error(`Error verifying JWT: ${err}`);
      return { verified: false, error: 'Error verifying JWT' };
    }
  }

  async getJwks(): Promise<any> {
    const res = await this.http.axiosRef.get(`${this.baseUrl}/.well-known/jwks.json`);
    return res.data;
  }
}
