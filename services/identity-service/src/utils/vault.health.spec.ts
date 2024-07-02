import { InternalServerErrorException, NotFoundException } from '@nestjs/common';
import { HealthCheckError } from '@nestjs/terminus';
import { VaultService } from './vault.service';
import { VaultHealthIndicator } from 'src/utils/vault.health';

describe('VaultHealthIndicator', () => {
  let vaultHealthIndicator: VaultHealthIndicator;
  let vaultService: VaultService;

  beforeEach(() => {
    vaultService = new VaultService(); // Mock or use a real instance of VaultService here
    vaultHealthIndicator = new VaultHealthIndicator(vaultService);
  });

  it('should be defined', () => {
    expect(vaultHealthIndicator).toBeDefined();
  });

  describe('isHealthy', () => {
    it('should return healthy result if Vault status is initialized and unsealed', async () => {
      jest.spyOn(vaultService, 'checkStatus').mockResolvedValueOnce({
        status: { sealed: false, initialized: true },
        vault_config: {}
      }); // Mock successful status check

      const result = await vaultHealthIndicator.isHealthy('vault');

      expect(result).toEqual({ vault: { status: 'up' } });
      expect(vaultService.checkStatus).toHaveBeenCalled(); // Assert that checkStatus was called
    });

    it('should throw InternalServerErrorException if Vault is sealed or not initialized', async () => {
      jest.spyOn(vaultService, 'checkStatus').mockResolvedValueOnce({
        status: { sealed: true, initialized: false },
        vault_config: {}
      }); // Mock sealed and not initialized status

      await expect(vaultHealthIndicator.isHealthy('vault')).rejects.toThrow(HealthCheckError);
      expect(vaultService.checkStatus).toHaveBeenCalled(); // Assert that checkStatus was called
    });

    it('should throw HealthCheckError if Vault status check fails', async () => {
      jest.spyOn(vaultService, 'checkStatus').mockRejectedValueOnce(new NotFoundException()); // Mock status check failure

      await expect(vaultHealthIndicator.isHealthy('vault')).rejects.toThrowError(HealthCheckError);
      expect(vaultService.checkStatus).toHaveBeenCalled(); // Assert that checkStatus was called
    });
  });
});
