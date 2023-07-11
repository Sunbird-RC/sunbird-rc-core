import {
  Body,
  CacheInterceptor,
  Controller,
  Get,
  Param,
  Post,
  Query,
  UseInterceptors,
  Put,
} from '@nestjs/common';
import {
  ApiBadRequestResponse,
  ApiBody,
  ApiCreatedResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiQuery,
} from '@nestjs/swagger';

import { CreateCredentialDTO } from './dto/create-credentials.dto';
import { VCItem } from './entities/VCItem.entity';
import { VCModelSchema } from './entities/VCModelSchema.entity';
import { SchemaService } from './schema.service';

@Controller('credential-schema')
@UseInterceptors(CacheInterceptor)
export class SchemaController {
  constructor(private readonly schemaService: SchemaService) {}

  // TODO: Add role based guards here
  @Get(':id/:ver')
  @ApiQuery({ name: 'id', required: true, type: String })
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
  getCredentialSchemaByIdAndVersion(
    @Param('id') id,
    @Param('ver') version: string,
  ) {
    return this.schemaService.getCredentialSchemaByIdAndVersion({
      id_version: {
        id,
        version,
      },
    });
  }

  @Get()
  @ApiQuery({ name: 'id', required: true, type: String })
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
  getCredentialSchemaByTags(
    @Query('tags') tags: string,
    @Query('page') page: string,
    @Query('limit') limit: string,
  ) {
    return this.schemaService.getSchemaByTags(
      tags.split(','),
      isNaN(parseInt(page, 10)) ? 1 : parseInt(page, 10),
      isNaN(parseInt(limit, 10)) ? 10 : parseInt(limit, 10),
    );
  }

  @Get(':id')
  async getAllSchemasWithId(@Param('id') id: string) {
    return this.schemaService.getAllSchemasById(id);
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
    @Body() body: CreateCredentialDTO, //: Promise<VerifiableCredentialSchema>
  ) {
    return this.schemaService.createCredentialSchema(body);
  }

  // TODO: Add role based guards here
  @Put(':id/:ver')
  @ApiQuery({ name: 'id', required: true, type: String })
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
      'The record with the passed query param id has not been found.',
  })
  @ApiBadRequestResponse({
    status: 400,
    description: 'There was some prioblem with the request.',
  })
  updateCredentialSchema(
    @Param('id') id: string,
    @Param('ver') version: string,
    @Body() data: CreateCredentialDTO,
  ) {
    return this.schemaService.updateCredentialSchema(
      { id_version: { id, version } },
      data,
    );
  }

  @Put('deprecate/:id/:ver')
  async deprecateSchema(
    @Param('id') id: string,
    @Param('ver') version: string,
  ) {
    return await this.schemaService.updateSchemaStatus(
      {
        id_version: { id, version },
      },
      'DEPRECATED',
    );
  }

  @Put('revoke/:id/:ver')
  async revokeSchema(@Param('id') id: string, @Param('ver') version: string) {
    return await this.schemaService.updateSchemaStatus(
      {
        id_version: { id, version },
      },
      'REVOKED',
    );
  }

  @Put('publish/:id/:ver')
  async publishSchema(@Param('id') id: string, @Param('ver') version: string) {
    return await this.schemaService.updateSchemaStatus(
      {
        id_version: { id, version },
      },
      'PUBLISHED',
    );
  }
}
