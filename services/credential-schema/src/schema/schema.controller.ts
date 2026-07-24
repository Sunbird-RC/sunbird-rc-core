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
export class SchemaController {
  constructor(private readonly schemaService: SchemaService) {}

  // Registered before the ':id' routes so 'oid4vci-configs' is not swallowed
  // by the single-segment @Get(':id') matcher.
  //
  // Deliberately NOT cached: oid4vc-service polls this endpoint live for both
  // issuer metadata (credential_configurations_supported) and offer creation
  // (POST /oid4vc/offer). This route has a single, unparameterized cache key,
  // so caching it here previously meant a schema newly opted into OID4VCI
  // could stay invisible to oid4vc-service indefinitely (until LRU eviction
  // or a restart) — offers for it would silently 404 as "not enabled".
  @Get('oid4vci-configs')
  @ApiOperation({ summary: 'List published schemas opted into OID4VCI' })
  @ApiOkResponse({ status: 200, description: 'OID4VCI-enabled schema configs' })
  getOid4vciConfigs() {
    return this.schemaService.getOid4vciConfigs();
  }

  // TODO: Add role based guards here
  @Get(':id/:ver')
  @UseInterceptors(CacheInterceptor)
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
  @UseInterceptors(CacheInterceptor)
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
  @UseInterceptors(CacheInterceptor)
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
