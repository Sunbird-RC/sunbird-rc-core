import { Controller, Get } from '@nestjs/common';
import { HealthCheck, HealthCheckService } from '@nestjs/terminus';
import { HealthCheckUtilsService } from './credentials/utils/healthcheck.utils.service';

@Controller()
export class AppController {
  constructor(
    private readonly healthCheckService: HealthCheckService,
    private readonly healthCheckUtils: HealthCheckUtilsService
  ) {}

  @Get('/health')
  @HealthCheck()
  public async checkHealth() {
    return this.healthCheckService.check([
      async () => this.healthCheckUtils.prismaIsHealthy('db'),
      async () => this.healthCheckUtils.identityIsHealthy('identity-service'), 
      async () => this.healthCheckUtils.credSchemaIsHealthy('credential-schema-service'), 
    ])
  }
}
