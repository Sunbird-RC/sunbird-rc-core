import { HttpService } from '@nestjs/axios';
import {
  HttpException,
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';

@Injectable()
export class AnchorCordService {
  constructor(private readonly httpService: HttpService) {}
  private logger = new Logger(AnchorCordService.name);
 
  async anchorDid(body: any): Promise<any> {
    try {
      const response = await this.httpService.axiosRef.post(
        `${process.env.ISSUER_AGENT_BASE_URL}/did/create/`,
        body,
      );
      return response.data;
    } catch (err) {
      const errorDetails = {
        message: err.message,
        status: err.response?.status,
        statusText: err.response?.statusText,
        data: err.response?.data,
        headers: err.response?.headers,
        request: err.config,
      };

      this.logger.error(
        'Error anchoring did to Cord blockchain',
        errorDetails,
      );
      throw new InternalServerErrorException(
        'Failed to anchor did to Cord blockchain',
      );
    }
  }
}
