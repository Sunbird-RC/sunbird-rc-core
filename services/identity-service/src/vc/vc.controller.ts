import { Body, Controller, Get, Param, Post } from '@nestjs/common';
import { SignJsonDTO } from './dtos/Sign.dto';
import { VerifyJsonDTO } from './dtos/Verify.dto';
import VcService from './vc.service';
import { ApiBadRequestResponse, ApiInternalServerErrorResponse, ApiOkResponse, ApiOperation, ApiTags } from '@nestjs/swagger';

@ApiTags('VC')
@Controller('utils')
export class VcController {
  constructor(private readonly VcService: VcService) {}

  @ApiOperation({ summary: 'Sign an unsigned VC' })
  @ApiOkResponse({ description: 'VC Signed' })
  @ApiBadRequestResponse({ description: 'Bad Request' })
  @ApiInternalServerErrorResponse({ description: 'Internal Server Error' })
  @Post('/sign')
  sign(@Body() body: SignJsonDTO) {
    return this.VcService.sign(body.DID, body.payload, body.isVerifiableCredential);
  }

  @ApiOperation({ summary: 'Verify a signed VC' })
  @Post('/verify')
  verify(@Body() body: VerifyJsonDTO) {
    return this.VcService.verify(body.DID, body.payload);
  }
}
