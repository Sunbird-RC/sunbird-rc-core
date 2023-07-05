import { Controller, Get } from '@nestjs/common';
import { HealthCheck, HealthCheckService } from '@nestjs/terminus';
import { PrismaHealthIndicator } from './utils/prisma.health';

@Controller()
export class AppController {
  constructor(
    private readonly healthCheckService: HealthCheckService,
    private readonly prismaIndicator: PrismaHealthIndicator,
  ) {}
  @Get('/health')
  @HealthCheck()
  public async checkHealth() {
    return this.healthCheckService.check([
      async () => this.prismaIndicator.isHealthy('db'),
    ]);
  }
}
