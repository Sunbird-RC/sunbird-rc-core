import { Controller, Get } from '@nestjs/common';
@Controller()
export class AppController {
  constructor() {}

  @Get()
  handleHealthCheck(): string {
    return 'Hello World!';
  }
}
