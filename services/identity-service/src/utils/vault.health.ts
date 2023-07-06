import { Injectable, InternalServerErrorException } from "@nestjs/common";
import { HealthCheckError, HealthIndicator, HealthIndicatorResult } from "@nestjs/terminus";
import { VaultService } from "./vault.service";

@Injectable()
export class VaultHealthIndicator extends HealthIndicator {
  constructor(private readonly vaultService: VaultService) {
    super();
  }
  async isHealthy(key: string): Promise<HealthIndicatorResult> {
    try {
      const resp = await this.vaultService.checkStatus();
      if (resp.status.sealed === false && resp.status.initialized === true) return this.getStatus(key, true);
      throw new InternalServerErrorException('Vault is not initialized or sealed');
    } catch (err) {
      throw new HealthCheckError("Prisma health check failed", err);
    }
  }
}