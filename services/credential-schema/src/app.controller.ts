import { Controller, Get } from '@nestjs/common';
import { AppService } from './app.service';
import { HealthCheck, HealthCheckService } from '@nestjs/terminus';
import { PrismaHealthIndicator } from './utils/prisma.health';

@Controller()
export class AppController {
  constructor(
    private readonly appService: AppService,
    private readonly healthCheckService: HealthCheckService,
    private readonly prismaIndicator: PrismaHealthIndicator,
  ) {}

  @Get()
  getHello(): string {
    return this.appService.getHello();
  }
  @Get('/health')
  @HealthCheck()
  public async checkHealth() {
    return this.healthCheckService.check([
      async () => this.prismaIndicator.isHealthy('db'),
    ]);
  }
}
