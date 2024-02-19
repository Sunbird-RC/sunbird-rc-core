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
