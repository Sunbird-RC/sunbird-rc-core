import {
  Body,
  CacheInterceptor,
  CACHE_MANAGER,
  Controller,
  Get,
  Inject,
  Param,
  Put,
  Post,
  Query,
  UseInterceptors,
} from '@nestjs/common';
import {
  ApiBadRequestResponse,
  ApiBody,
  ApiCreatedResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiQuery,
} from '@nestjs/swagger';
import { Status as PrismaStatus, VerifiableCredentialSchema } from '@prisma/client';
import { Cache } from 'cache-manager';
import { type } from 'os';

import { VCSModelSchemaInterface } from 'src/types/VCModelSchema.interface';
import { CreateCredentialDTO } from './dto/create-credentials.dto';
import { VCItem } from './entities/VCItem.entity';
import { VCModelSchema } from './entities/VCModelSchema.entity';
import { SchemaService } from './schema.service';

type schemaResponse = {
  schema: {
    // type: string,
    // id: string, 
    // version: string,
    // name: string,
    // author: string,
    // authored: string,
    // schema: {

    // }
    // proof: {

    // }
  }
  tags: string[], 
  status: PrismaStatus, 
  createdAt: string,
  createdBy: string,
  updatedAt: string,
  updatedBy: string,
}

@Controller('credential-schema')
@UseInterceptors(CacheInterceptor)
export class SchemaController {
  constructor(
    private readonly schemaService: SchemaService,
    @Inject(CACHE_MANAGER) private cacheManager: Cache,
  ) {}

  // this should be public
  // @Get(':id')
  // @ApiParam({
  //   name: 'id',
  //   required: true,
  //   type: String,
  //   description: 'ID of the json schema files stored on the server',
  // })
  // @ApiOkResponse({ type: VCModelSchema })
  // @ApiNotFoundResponse({
  //   status: 404,
  //   description:
  //     'The record with the passed query param id has not been found.',
  // })
  // getSchema(@Param('id') id: string) {
  //   return this.schemaService.getSchema(id);
  // }

  // TODO: Add role based guards here
  @Get(':id')
  // @ApiQuery({ name: 'id', required: true, type: String })
  @ApiOperation({ summary: 'Get a Verifiable Credential Schema by id (did)' })
  @ApiOkResponse({
    status: 200,
    description: 'The record has been successfully created.',
    type: VCItem,
  })
  @ApiNotFoundResponse({
    status: 404,
    description: 'The record has not been found.',
  })
  getCredentialSchema(@Param('id') id: string) {
    return this.schemaService.credentialSchema({ id: id });
  }

  @Get()
  @ApiQuery({ name: 'tags', required: true, type: String })
  @ApiOperation({ summary: 'Get a Verifiable Credential Schema by tags' })
  @ApiOkResponse({
    status: 200,
    description: 'The record has been successfully obtained',
    type: VCItem,
  })
  @ApiNotFoundResponse({
    status: 404,
    description: 'The record has not been found.',
  })
  getCredentialSchemaByTags(@Query('tags') tags: string){
    console.log(tags)
    return this.schemaService.getSchemaByTags(
      tags.split(','),
    );
  }

  // TODO: Add role based guards here
  @Post()
  @ApiOperation({ summary: 'Create a new Verifiable Credential Schema' })
  @ApiBody({
    type: VCModelSchema,
  })
  @ApiCreatedResponse({
    status: 201,
    description: 'The record has been successfully created.',
    type: VCItem,
  })
  @ApiBadRequestResponse({
    status: 400,
    description: 'There was some problem with the request.',
  })
  createCredentialSchema(
    @Body() body: CreateCredentialDTO,
  ): Promise<schemaResponse> {
    console.log(body);
    return this.schemaService.createCredentialSchema(body);
  }

  // TODO: Add role based guards here
  @Put(':id')
  // @ApiQuery({ name: 'id', required: true, type: String })
  @ApiBody({
    type: VCModelSchema,
  })
  @ApiOperation({
    summary: 'Update a Verifiable Credential Schema by id (did)',
  })
  @ApiOkResponse({
    status: 200,
    description: 'The record has been successfully updated.',
    type: VCItem,
  })
  @ApiNotFoundResponse({
    status: 404,
    description:
      'The record with the passed param id has not been found.',
  })
  @ApiBadRequestResponse({
    status: 400,
    description: 'There was some problem with the request.',
  })
  updateCredentialSchema(
    @Param('id') id,
    @Body() data: VCSModelSchemaInterface,
  ) {
    return this.schemaService.updateCredentialSchema({
      where: { id: id },
      data,
    });
  }
}
