import { HttpService } from '@nestjs/axios';
import {
  BadRequestException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { AxiosResponse } from 'axios';
import { DIDDocument } from 'did-resolver';
import { lastValueFrom } from 'rxjs';
import { DidService } from 'src/did/did.service';
import { GenerateDidDTO } from 'src/did/dtos/GenerateDid.dto';

@Injectable()
export default class KycService {
  constructor(
    private readonly httpService: HttpService,
    private readonly didService: DidService,
  ) { }

  async triggerKyc(aadhaar: string) {
    const userData = {
      email: aadhaar,
    };
    let response: AxiosResponse<any, any>;
    try {
      response = await lastValueFrom(
        this.httpService.post(
          'http://64.227.185.154:8000/otp/generate/',
          userData,
        ),
      );
    } catch (err) {
      console.log(err);
      throw new BadRequestException();
    }
    if (response.data) {
      if (response.data.status == 200) return { status: 200, error: '' };
      else throw new UnauthorizedException(response.data.data);
    } else {
      throw new UnauthorizedException('Cannot fulfil request!');
    }
  }

  async register(
    aadhaar: string,
    otp: number,
    username: string,
    password: string,
  ): Promise<DIDDocument> {
    // TODO: sanitize all inputs
    const userData = {
      email: aadhaar,
      otp: otp,
    };
    console.log(userData);
    const response = await lastValueFrom(
      this.httpService.post('http://64.227.185.154:8000/otp/verify/', userData),
    );
    console.log(response);
    if (response.data) {
      if (response.data.status == 200) {
        console.log('Verified');
        const registrationData = {
          registration: {
            generateAuthenticationToken: true,
            applicationId: process.env.APPLICATION_ID,
            roles: ['Student'],
          },
          user: {
            username: username,
            password: password,
          },
        };

        const headers = {
          Authorization: process.env.FUSION_API_KEY,
        };
        console.log(headers);
        try {
          const fusionResponse = await lastValueFrom(
            this.httpService.post(
              'https://auth.konnect.samagra.io/api/user/registration/',
              registrationData,
              { headers: headers },
            ),
          );
          const registrationDid = await this.createRegistrationDid(
            aadhaar,
            username,
          );
          console.log({ fusionResponse, registrationDid });
          // return { fusionResponse, registrationDid };
          return registrationDid;
        } catch (err) {
          return err;
        }
      } else throw new UnauthorizedException(response.data.data);
    } else {
      throw new UnauthorizedException('Cannot fulfil request!');
    }
  }

  private async createRegistrationDid(
    aadhaar: string,
    username: string,
  ): Promise<DIDDocument> {
    const didContent: GenerateDidDTO = {
      alsoKnownAs: [`did:${aadhaar}:${aadhaar}`, aadhaar, username],
      service: [
        {
          id: 'AadhaarAuthentication',
          type: 'AadhaarAuthentication',
          serviceEndpoint: 'http://64.227.185.154:8000',
        },
        {
          id: 'OIDCAuthentication',
          type: 'OIDCAuthentication',
          serviceEndpoint: 'http://auth.konnect.samagra.io',
        },
      ],
    };

    try {
      const generatedDid = await this.didService.generateDID(didContent);
      return generatedDid;
    } catch (err) {
      throw new BadRequestException();
    }
  }
}
