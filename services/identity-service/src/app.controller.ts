import { Controller, Get, Post } from '@nestjs/common';
import { HealthCheck, HealthCheckService, HttpHealthIndicator } from '@nestjs/terminus';
import { PrismaHealthIndicator } from './utils/prisma.health';
import { SecretsHealthIndicator } from './utils/secrets.health';

@Controller()
export class AppController {
  constructor(
    private readonly healthCheckService: HealthCheckService,
    private readonly prismaIndicator: PrismaHealthIndicator,
    private readonly secretsIndicator: SecretsHealthIndicator,
    private readonly http: HttpHealthIndicator
  ) {}
  @Get('/health')
  @HealthCheck()
  public async checkHealth() {
    return this.healthCheckService.check([
      async () => this.prismaIndicator.isHealthy('db'),
      async () => this.secretsIndicator.isHealthy('secrets'),
    ])
  }
}
