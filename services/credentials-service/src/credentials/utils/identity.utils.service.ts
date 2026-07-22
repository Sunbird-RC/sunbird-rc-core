import { HttpService } from '@nestjs/axios';
import { AxiosResponse } from '@nestjs/terminus/dist/health-indicator/http/axios.interfaces';
import { DIDDocument, CredentialPayload, IssuerType } from 'vc.types';
import {
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';
import { W3CCredential } from 'vc.types';

@Injectable()
export class IdentityUtilsService {
  identityBaseUrl: string;
  constructor(private readonly httpService: HttpService) {
    this.identityBaseUrl = process.env.IDENTITY_BASE_URL;
  }

  private logger = new Logger(IdentityUtilsService.name);

  async signVC(
    credentialPlayload: CredentialPayload,
    did: IssuerType
  ): Promise<W3CCredential> {
    try {
      const signedVCResponse: AxiosResponse =
        await this.httpService.axiosRef.post(
          `${process.env.IDENTITY_BASE_URL}/utils/sign`,
          {
            DID: did,
            payload: credentialPlayload,
          }
        );
        return signedVCResponse.data as W3CCredential;
    } catch (err) {
      console.log(err);
      this.logger.error('Error signing VC: ', err);
      throw new InternalServerErrorException('Error signing VC');
    }
  }

  // Signs an arbitrary payload as a compact JWS (ES256) via identity-service.
  // Used for the jwt_vc_json format.
  async signJwt(
    did: IssuerType,
    payload: object,
    header: Record<string, any> = {}
  ): Promise<string> {
    try {
      const res: AxiosResponse = await this.httpService.axiosRef.post(
        `${this.identityBaseUrl}/utils/sign-jwt`,
        { DID: did, payload, header }
      );
      return res.data?.jwt as string;
    } catch (err) {
      this.logger.error('Error signing JWT: ', err);
      throw new InternalServerErrorException('Error signing JWT');
    }
  }

  // Verifies a compact JWS via identity-service.
  async verifyJwt(
    jwt: string,
    did?: string
  ): Promise<{ verified: boolean; payload?: any; error?: string }> {
    try {
      const res: AxiosResponse = await this.httpService.axiosRef.post(
        `${this.identityBaseUrl}/utils/verify-jwt`,
        { jwt, DID: did }
      );
      return res.data;
    } catch (err) {
      this.logger.error('Error verifying JWT: ', err);
      return { verified: false, error: 'Error verifying JWT' };
    }
  }

  // Signs an SD-JWT (vc+sd-jwt) via identity-service.
  async signSdJwt(
    did: IssuerType,
    payload: Record<string, any>,
    disclosable: string[] = [],
    header: Record<string, any> = {}
  ): Promise<string> {
    try {
      const res: AxiosResponse = await this.httpService.axiosRef.post(
        `${this.identityBaseUrl}/utils/sign-sd-jwt`,
        { DID: did, payload, disclosable, header }
      );
      return res.data?.sdJwt as string;
    } catch (err) {
      this.logger.error('Error signing SD-JWT: ', err);
      throw new InternalServerErrorException('Error signing SD-JWT');
    }
  }

  // Verifies an SD-JWT via identity-service, optionally checking key binding.
  async verifySdJwt(
    sdJwt: string,
    did?: string,
    keyBinding?: { nonce?: string; audience?: string }
  ): Promise<{ verified: boolean; claims?: Record<string, any>; error?: string }> {
    try {
      const res: AxiosResponse = await this.httpService.axiosRef.post(
        `${this.identityBaseUrl}/utils/verify-sd-jwt`,
        { sdJwt, DID: did, keyBinding }
      );
      return res.data;
    } catch (err) {
      this.logger.error('Error verifying SD-JWT: ', err);
      return { verified: false, error: 'Error verifying SD-JWT' };
    }
  }

  async resolveDID(issuer: string): Promise<DIDDocument> {
    try {
      let url = `${this.identityBaseUrl}/did/resolve/${encodeURIComponent(issuer)}`;
      const dIDResponse: AxiosResponse = await this.httpService.axiosRef.get(
        url
      );
      return dIDResponse.data as DIDDocument;
    } catch (err) {
      this.logger.error('Error resolving DID: ', err);
      throw new InternalServerErrorException('Error resolving DID');
    }
  }

  async generateDID(
    alsoKnownAs: ReadonlyArray<String>,
    method: string = 'rcw'
  ): Promise<ReadonlyArray<DIDDocument>> {
    try {
      const didGenRes: AxiosResponse = await this.httpService.axiosRef.post(
        `${process.env.IDENTITY_BASE_URL}/did/generate`,
        {
          content: [
            {
              alsoKnownAs: alsoKnownAs,
              services: [
                {
                  id: 'CredentialsService',
                  type: 'CredentialDIDService',
                },
              ],
              method,
            },
          ],
        }
      );
      return didGenRes.data as ReadonlyArray<DIDDocument>;
    } catch (err) {
      this.logger.error('Error generating DID: ', err);
      throw new InternalServerErrorException('Error generating DID');
    }
  }
}
