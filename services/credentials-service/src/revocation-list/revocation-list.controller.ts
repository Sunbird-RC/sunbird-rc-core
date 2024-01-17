import {
    Controller,
    Get
  } from '@nestjs/common';
  import { RevocationListService } from './revocation-list.service';

  @Controller('revocation')
  export class RevocationListController {
    constructor(private readonly revocationListService: RevocationListService) {}
  
    @Get('revocation-list')
    getRevocationList() {

      return this.revocationListService.getRevocationList();
    }
  
  }
  