import { HttpService } from '@nestjs/axios';
import { AxiosResponse } from '@nestjs/terminus/dist/health-indicator/http/axios.interfaces';
import {
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';
import { W3CCredential, Verifiable } from 'vc.types';
@Injectable()
export class AnchorCordUtilsServices {
  private ISSUER_AGENT_BASE_URL: string;
  private VERIFICATION_MIDDLEWARE_BASE_URL: string

  constructor(private readonly httpService: HttpService) {
    this.ISSUER_AGENT_BASE_URL = process.env.ISSUER_AGENT_BASE_URL;
    this.VERIFICATION_MIDDLEWARE_BASE_URL = process.env.VERIFICATION_MIDDLEWARE_BASE_URL;
  }

  private logger = new Logger(AnchorCordUtilsServices.name);

  async anchorCredential(credentialPayload: any): Promise<any> {
    try {
      this.logger.debug('url', this.ISSUER_AGENT_BASE_URL);
      const anchorResponse: AxiosResponse =
        await this.httpService.axiosRef.post(
          `${this.ISSUER_AGENT_BASE_URL}/cred`,
          {
            credential: credentialPayload,
          }
        );

      this.logger.debug('Credential successfully anchored');
      return anchorResponse.data;
    } catch (err) {
      this.logger.error('Error anchoring credential:', err);

      throw new InternalServerErrorException(`Error anchoring credential : ${err.response.data.details}`);
    }
  }

  async verifyCredentialOnCord(
    credToVerify: Verifiable<W3CCredential>
  ): Promise<any> {
    try {
      const response = await this.httpService.axiosRef.post(
        `${this.VERIFICATION_MIDDLEWARE_BASE_URL}/credentials/verify`,
        credToVerify
      );

      if (response.status !== 200) {
        this.logger.error('Cord verification failed:', response.data);
        throw new InternalServerErrorException('Cord verification failed');
      }

      return response.data;
    } catch (err) {
      this.logger.error('Error calling Cord verification API:', err);
      throw new InternalServerErrorException(
        'Error verifying credential on Cord'
      );
    }
  }
}
