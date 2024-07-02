import { InternalServerErrorException, Logger } from '@nestjs/common';
import { VaultService } from './vault.service';
const Vault = require('hashi-vault-js');

jest.mock('hashi-vault-js');

describe('VaultService', () => {
  let vaultService: VaultService;
  let mockVault: any;

  beforeEach(() => {
    vaultService = new VaultService();
    mockVault = new Vault();
    (vaultService as any).vault = mockVault;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should be defined', () => {
    expect(vaultService).toBeDefined();
  });

  describe('checkStatus', () => {
    it('should return status and vault_config', async () => {
      const status = { sealed: false, initialized: true };
      const vault_config = { example: 'config' };
      jest.spyOn(mockVault, 'healthCheck').mockResolvedValueOnce(status);
      jest.spyOn(mockVault, 'readKVEngineConfig').mockResolvedValueOnce(vault_config);

      const result = await vaultService.checkStatus();

      expect(result).toEqual({ status, vault_config });
      expect(mockVault.healthCheck).toHaveBeenCalled();
      expect(mockVault.readKVEngineConfig).toHaveBeenCalledWith((vaultService as any).token);
    });

    it('should handle errors and return default values', async () => {
      jest.spyOn(mockVault, 'healthCheck').mockRejectedValueOnce(new Error('Health check failed'));
      jest.spyOn(mockVault, 'readKVEngineConfig').mockRejectedValueOnce(new Error('Config read failed'));
      const loggerErrorSpy = jest.spyOn(Logger, 'error').mockImplementation();

      const result = await vaultService.checkStatus();

      expect(result).toEqual({ status: undefined, vault_config: undefined });
      expect(loggerErrorSpy).toHaveBeenCalledTimes(2);
      expect(loggerErrorSpy).toHaveBeenCalledWith('Error in checking vault status: Error: Health check failed');
      expect(loggerErrorSpy).toHaveBeenCalledWith('Error in checking vault config: Error: Config read failed');
    });
  });

  describe('writePvtKey', () => {
    it('should write private key to vault', async () => {
      const secret = { key: 'value' };
      const name = 'example';
      const path = 'path/to/secret';

      jest.spyOn(mockVault, 'createKVSecret').mockResolvedValueOnce(secret);

      const result = await vaultService.writePvtKey(secret, name, path);

      expect(result).toEqual(secret);
      expect(mockVault.createKVSecret).toHaveBeenCalledWith((vaultService as any).token, `${path}/${name}`, secret);
    });

    it('should handle errors and throw InternalServerErrorException', async () => {
      const secret = { key: 'value' };
      const name = 'example';
      const path = 'path/to/secret';

      jest.spyOn(mockVault, 'createKVSecret').mockRejectedValueOnce(new Error('Write failed'));
      const loggerErrorSpy = jest.spyOn(Logger, 'error').mockImplementation();

      await expect(vaultService.writePvtKey(secret, name, path)).rejects.toThrowError(InternalServerErrorException);

      expect(mockVault.createKVSecret).toHaveBeenCalledWith((vaultService as any).token, `${path}/${name}`, secret);
      expect(loggerErrorSpy).toHaveBeenCalledWith(new Error('Write failed'));
    });
  });

  describe('readPvtKey', () => {
    it('should read private key from vault', async () => {
      const name = 'example';
      const path = 'path/to/secret';
      const data = { key: 'value' };

      jest.spyOn(mockVault, 'readKVSecret').mockResolvedValueOnce({ data });

      const result = await vaultService.readPvtKey(name, path);

      expect(result).toEqual(data);
      expect(mockVault.readKVSecret).toHaveBeenCalledWith((vaultService as any).token, `${path}/${name}`);
    });
  });
});
