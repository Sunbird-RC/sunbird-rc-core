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
  webDidBaseUrl: string;
  identityBaseUrl: string;
  parse: any;
  constructor(private readonly httpService: HttpService) {
    this.webDidBaseUrl = process.env.WEB_DID_BASE_URL;
    this.identityBaseUrl = process.env.IDENTITY_BASE_URL;
    this.init();
  }

  async init() {
    const { parse } = await import('did-resolver');
    this.parse = parse;
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

  async resolveDID(issuer: string): Promise<DIDDocument> {
    try {
      let url = `${this.identityBaseUrl}/did/resolve/${issuer}`;
      if(issuer?.startsWith("did:web")) {
        url = await this.getWebDidUrl(issuer);
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

  async getWebDidUrl(did: string): Promise<string> {
    if(!this.parse) await this.init();
    const parsed = this.parse(did);
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
