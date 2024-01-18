import {
  Body,
    Controller,
    Get,
    Post,
    Query
  } from '@nestjs/common';
  import { RevocationListService } from './revocation-list.service';
import { GetRevocationListByIssuer } from 'src/credentials/dto/getRevocationListByIssuer.dto';
import { ApiTags } from '@nestjs/swagger';

  @Controller('revocation')
  export class RevocationListController {
    constructor(private readonly revocationListService: RevocationListService) {}
  
    @ApiTags('Issuing')
    @Post('/revocation-list')
    getRevocationList(
      @Body() getRevocationList: GetRevocationListByIssuer,
      @Query('page') page: string,
      @Query('limit') limit: string
    ) {

      return this.revocationListService.getRevocationList(
        getRevocationList,
        isNaN(parseInt(page)) ? 1 : parseInt(page),
        isNaN(parseInt(limit)) ? 10 : parseInt(limit)
      );
    }
  
  }
  