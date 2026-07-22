import { Controller, Get } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';

@ApiTags('Health')
@Controller()
export class AppController {
  @ApiOperation({ summary: 'Liveness probe' })
  @Get('health')
  health() {
    return { status: 'UP', service: 'oid4vc-service' };
  }
}
