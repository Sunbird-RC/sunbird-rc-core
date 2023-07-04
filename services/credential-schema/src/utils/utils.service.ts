import { HttpService } from '@nestjs/axios';
import { HttpException, Injectable, Logger } from '@nestjs/common';

@Injectable()
export class UtilsService {
  constructor(private readonly httpService: HttpService) {}
  private logger = new Logger(UtilsService.name);
  async sign(did: string, body: any) {
    try {
      const signedVCResponse = await this.httpService.axiosRef.post(
        `${process.env.IDENTITY_BASE_URL}/utils/sign`,
        {
          DID: did,
          payload: body,
        },
      );

      const proof = {
        proofValue: signedVCResponse.data.signed as string,
        proofPurpose: 'assertionMethod',
        created: new Date().toISOString(),
        type: 'Ed25519Signature2020',
        verificationMethod: did,
      };
      return proof;
    } catch (err) {
      this.logger.error(err, err.response.data);
      throw new HttpException("Couldn't sign the schema", 500);
    }
  }
}
