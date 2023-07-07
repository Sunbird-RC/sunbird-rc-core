import { Controller, Get } from '@nestjs/common';
@Controller()
export class AppController {
  constructor() {}

  @Get('/health')
  handleHealthCheck() {
    return { status: 'ok' };
  }
}
