import { Injectable } from '@nestjs/common';
import {
  HealthCheckError,
  HealthIndicator,
  HealthIndicatorResult,
} from '@nestjs/terminus';
import { SecretsProvider } from '../secrets/secrets-provider';

@Injectable()
export class SecretsHealthIndicator extends HealthIndicator {
  constructor(private readonly secretsProvider: SecretsProvider) {
    super();
  }

  async isHealthy(key: string): Promise<HealthIndicatorResult> {
    try {
      const resp = await this.secretsProvider.checkStatus();
      if (resp.healthy) {
        return this.getStatus(key, true, { provider: resp.provider });
      }
      throw new HealthCheckError(
        `${resp.provider} secrets health check failed`,
        resp.details,
      );
    } catch (err) {
      throw new HealthCheckError('Secrets health check failed', err);
    }
  }
}
