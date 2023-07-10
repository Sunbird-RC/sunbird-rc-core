import { Controller, Get } from '@nestjs/common';
import { HealthCheck, HealthCheckService, HttpHealthIndicator } from '@nestjs/terminus';
import { PrismaHealthIndicator } from './utils/prisma.health';

@Controller()
export class AppController {
  constructor(
    private readonly healthCheckService: HealthCheckService,
    private readonly prismaIndicator: PrismaHealthIndicator,
    private readonly http: HttpHealthIndicator
  ) {}
  @Get('/health')
  @HealthCheck()
  public async checkHealth() {
    return this.healthCheckService.check([
      async () => this.prismaIndicator.isHealthy('db'),
      async () => this.http.responseCheck('identity-service', `${process.env.IDENTITY_BASE_URL}/health`, (res) => res.status === 200)
    ]);
  }
}
