import {
  Body,
  Controller,
  Get,
  NotFoundException,
  Param,
  Patch,
  Post,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBadRequestResponse,
  ApiBody,
  ApiCreatedResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { DIDDocument } from 'did-resolver';
import { DidService } from './did.service';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { JwtAuthGuard } from './roles.guard';

@ApiTags('DID')
@Controller('did')
export class DidController {
  constructor(private readonly didService: DidService) {}

  @ApiOperation({ summary: 'Generate a new DID' })
  @ApiOkResponse({ description: 'DID Generated', isArray: true })
  @ApiBadRequestResponse({ description: 'Bad request' })
  @ApiBody({ type: GenerateDidDTO, isArray: true })
  @Post('/generate')
  async generateDID(
    @Body() generateRequest: { content: GenerateDidDTO[] },
  ): Promise<DIDDocument[]> {
    const generatedDIDs: Array<DIDDocument> = [];
    // TODO: Handle failed DIDs
    for (const generateDidDTO of generateRequest.content) {
      generatedDIDs.push(await this.didService.generateDID(generateDidDTO));
    }
    return generatedDIDs;
  }

  @ApiOperation({ summary: 'Resolve a DID ID' })
  @ApiOkResponse({ description: 'DID resolved' })
  @ApiBadRequestResponse({ description: 'Bad Request' })
  @ApiNotFoundResponse({ description: 'DID not found' })
  @ApiParam({ name: 'id', description: 'The DID ID to resolve' })
  @Get('/resolve/:id')
  async resolveDID(@Param('id') id: string): Promise<DIDDocument> {
    const did: DIDDocument = await this.didService.resolveDID(id);
    if (did) {
      console.log('did in did controller: ', did);
      return did;
    } else {
      throw new NotFoundException('DID could not be resolved!');
    }
  }

  // @Patch('/update/:id')
  // @UseGuards(JwtAuthGuard)
  // async updateDID(@Param('id') id: string, @Body() body: any) {
  //   return this.didService.updateDID(id, body);
  // }
}
