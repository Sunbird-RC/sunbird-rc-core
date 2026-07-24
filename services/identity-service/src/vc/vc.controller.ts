import { Body, Controller, Post } from '@nestjs/common';
import { SignJsonDTO } from './dtos/Sign.dto';
import { VerifyJsonDTO } from './dtos/Verify.dto';
import {
  SignJwtDTO,
  VerifyJwtDTO,
  SignSdJwtDTO,
  VerifySdJwtDTO,
} from './dtos/SignJwt.dto';
import VcService from './vc.service';
import { JwtSignerService } from './jwt.service';
import { MdocService } from './mdoc.service';
import { ApiBadRequestResponse, ApiInternalServerErrorResponse, ApiOkResponse, ApiOperation, ApiTags } from '@nestjs/swagger';

@ApiTags('VC')
@Controller('utils')
export class VcController {
  constructor(
    private readonly VcService: VcService,
    private readonly jwtSigner: JwtSignerService,
    private readonly mdocSigner: MdocService,
  ) {}

  @ApiOperation({ summary: 'Sign an unsigned VC' })
  @ApiOkResponse({ description: 'VC Signed' })
  @ApiBadRequestResponse({ description: 'Bad Request' })
  @ApiInternalServerErrorResponse({ description: 'Internal Server Error' })
  @Post('/sign')
  sign(@Body() body: SignJsonDTO) {
    return this.VcService.sign(body.DID, body.payload);
  }

  @ApiOperation({ summary: 'Verify a signed VC' })
  @Post('/verify')
  verify(@Body() body: VerifyJsonDTO) {
    return this.VcService.verify(body.DID, body.payload);
  }

  @ApiOperation({ summary: 'Sign a payload as a compact JWS/JWT (ES256)' })
  @ApiOkResponse({ description: 'Signed JWT returned' })
  @Post('/sign-jwt')
  async signJwt(@Body() body: SignJwtDTO) {
    const jwt = await this.jwtSigner.signJwt(body.DID, body.payload, body.header || {});
    return { jwt };
  }

  @ApiOperation({ summary: 'Verify a compact JWS/JWT against a DID document' })
  @Post('/verify-jwt')
  verifyJwt(@Body() body: VerifyJwtDTO) {
    return this.jwtSigner.verifyJwt(body.jwt, body.DID);
  }

  @ApiOperation({ summary: 'Sign an SD-JWT with selectively disclosable claims' })
  @ApiOkResponse({ description: 'SD-JWT returned' })
  @Post('/sign-sd-jwt')
  async signSdJwt(@Body() body: SignSdJwtDTO) {
    const sdJwt = await this.jwtSigner.signSdJwt(
      body.DID,
      body.payload,
      body.disclosable || [],
      body.header || {},
    );
    return { sdJwt };
  }

  @ApiOperation({ summary: 'Verify an SD-JWT and reconstruct disclosed claims' })
  @Post('/verify-sd-jwt')
  verifySdJwt(@Body() body: VerifySdJwtDTO) {
    return this.jwtSigner.verifySdJwt(body.sdJwt, body.DID, body.keyBinding);
  }

  @ApiOperation({ summary: 'Sign an mso_mdoc (ISO/IEC 18013-5) credential' })
  @ApiOkResponse({ description: 'base64url-encoded CBOR MDoc returned' })
  @Post('/sign-mdoc')
  async signMdoc(
    @Body()
    body: {
      DID: string;
      docType: string;
      namespaces: Record<string, Record<string, any>>;
      deviceKeyJwk?: Record<string, any>;
    },
  ) {
    const mdoc = await this.mdocSigner.signMdoc(
      body.DID,
      body.docType,
      body.namespaces,
      body.deviceKeyJwk,
    );
    return { mdoc };
  }

  @ApiOperation({ summary: 'Verify a standalone mso_mdoc credential (no device/presentation context)' })
  @Post('/verify-mdoc')
  verifyMdoc(@Body() body: { mdoc: string }) {
    return this.mdocSigner.verifyMdoc(body.mdoc);
  }
}
