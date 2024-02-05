import { HttpService } from '@nestjs/axios';
import { JwtCredentialPayload } from 'did-jwt-vc';
import { IssuerType } from 'did-jwt-vc/lib/types';
import { AxiosResponse } from '@nestjs/terminus/dist/health-indicator/http/axios.interfaces';
import { DIDDocument } from 'did-resolver';
import {
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';
import { parse } from 'did-resolver';

@Injectable()
export class IdentityUtilsService {
  webDidBaseUrl: string;
  identityBaseUrl: string;
  constructor(private readonly httpService: HttpService) {
    this.webDidBaseUrl = process.env.WEB_DID_BASE_URL;
    this.identityBaseUrl = process.env.IDENTITY_BASE_URL;
  }

  private logger = new Logger(IdentityUtilsService.name);

  async signVC(
    credentialPlayload: JwtCredentialPayload,
    did: IssuerType
  ): Promise<String> {
    try {
      const signedVCResponse: AxiosResponse =
        await this.httpService.axiosRef.post(
          `${process.env.IDENTITY_BASE_URL}/utils/sign`,
          {
            DID: did,
            payload: JSON.stringify(credentialPlayload),
          }
        );
        const {publicKey, ...rest} = signedVCResponse.data;
        return rest;
    } catch (err) {
      this.logger.error('Error signing VC: ', err);
      throw new InternalServerErrorException('Error signing VC');
    }
  }

  async resolveDID(issuer: string): Promise<DIDDocument> {
    try {
      let url = `${this.identityBaseUrl}/did/resolve/${issuer}`;
      if(issuer?.startsWith("did:web")) {
        url = this.getWebDidUrl(issuer);
      }
      if(!!this.webDidBaseUrl && !!this.identityBaseUrl && url?.startsWith(this.webDidBaseUrl)) {
        url = url.replace(this.webDidBaseUrl, this.identityBaseUrl);
      }
      this.logger.debug("fetching did for url: " + url); 
      return this.getDID(url);
    } catch (err) {
      this.logger.error('Error resolving DID: ', err);
      throw new InternalServerErrorException('Error resolving DID');
    }
  }

  getWebDidUrl(did: string): string {
    const parsed = parse(did);
    let path = decodeURIComponent(parsed.id) + "/.well-known/did.json";
    const id = parsed.id.split(':')
    if (id.length > 1) {
      path = id.map(decodeURIComponent).join('/') + '/did.json'
    }
    return `https://${path}`;
  }

  async getDID(verificationURL): Promise<DIDDocument> {
    const dIDResponse: AxiosResponse = await this.httpService.axiosRef.get(
      verificationURL
    );
    return dIDResponse.data as DIDDocument;
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
