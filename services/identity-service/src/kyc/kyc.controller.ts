import { BadRequestException, Body, Controller, Get, Param, Post } from '@nestjs/common';
import KycService from './kyc.service';
import { ApiBadRequestResponse, ApiInternalServerErrorResponse, ApiOkResponse, ApiOperation, ApiTags, ApiUnauthorizedResponse } from '@nestjs/swagger';
import { TriggerKycDTO } from './dtos/TriggerKycDTO.dto';
import { RegisterUserDTO } from './dtos/RegisterUserDTO.dto';
import { DIDDocument } from 'did-resolver';

@ApiTags('KYC')
@Controller('kyc')
export class KycController {
  constructor(private readonly kycService: KycService) { }

  @ApiOperation({summary: 'Trigger OTP generation'})
  @ApiOkResponse({ description: 'OTP generated' })
  @ApiBadRequestResponse({ description: 'Bad request' })
  @ApiInternalServerErrorResponse({ description: 'Internal Server Error' })
  @Post('/triggerKyc')
  triggerKyc(@Body() triggerKycDTO: TriggerKycDTO) {
    return this.kycService.triggerKyc(triggerKycDTO.aadhaar);
  }

  @ApiOperation({ summary: 'Register a user' })
  @ApiOkResponse({ description: 'User Created' })
  @ApiBadRequestResponse({ description: 'Bad request' })
  @ApiUnauthorizedResponse({ description: 'Unauthorized' })
  @ApiInternalServerErrorResponse({ description: 'Internal Server Error' })
  @Post('/register')
  async register(@Body() registerUserDTO: RegisterUserDTO): Promise<DIDDocument> {
    return this.kycService.register(
      registerUserDTO.aadhaar,
      registerUserDTO.otp,
      registerUserDTO.username,
      registerUserDTO.password,
    );
  }
}
