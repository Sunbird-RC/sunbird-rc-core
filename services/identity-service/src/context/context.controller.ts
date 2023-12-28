import {
    Controller,
    Get,
    Param,
  } from '@nestjs/common';
  import {
    ApiBadRequestResponse,
    ApiNotFoundResponse,
    ApiOkResponse,
    ApiOperation,
    ApiParam,
    ApiTags,
  } from '@nestjs/swagger';
  import { ContextService } from './context.service';
  
  @ApiTags('Context')
  @Controller('context')
  export class ContextController {
    constructor(private readonly contextService: ContextService) {}
  
    @ApiOperation({ summary: 'Get a context by ID' })
    @ApiOkResponse({ description: 'Context Fetched' })
    @ApiBadRequestResponse({ description: 'Bad Request' })
    @ApiNotFoundResponse({ description: 'Context not found' })
    @ApiParam({ name: 'id', description: 'The Context ID to fetch' })
    @Get('/:id.json')
    async getContextById(@Param('id') id: string): Promise<Object> {
      return await this.contextService.getContextById(id);
    }
  }
  