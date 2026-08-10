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

  describe('mergePvtKey', () => {
    const name = 'example';
    const path = 'rcw/identity/private_keys';
    const secretPath = `${path}/${name}`;

    // hashi-vault-js's readKVSecret rejects (not resolves) on failure, tagging
    // the thrown error `isVaultError: true` with the original response.status.
    const vaultError = (statusCode: number) => {
      const err: any = new Error(`vault error ${statusCode}`);
      err.isVaultError = true;
      err.response = { status: statusCode };
      return err;
    };

    it('creates a fresh secret when none exists yet (404)', async () => {
      jest.spyOn(mockVault, 'readKVSecret').mockRejectedValueOnce(vaultError(404));
      jest.spyOn(mockVault, 'createKVSecret').mockResolvedValueOnce({ ok: true });

      const result = await vaultService.mergePvtKey({ newKey: 'v' }, name, path);

      expect(result).toEqual({ ok: true });
      expect(mockVault.createKVSecret).toHaveBeenCalledWith(
        (vaultService as any).token,
        secretPath,
        { newKey: 'v' },
      );
    });

    it('merges into the existing secret when one is found', async () => {
      jest.spyOn(mockVault, 'readKVSecret').mockResolvedValueOnce({
        data: { existingKey: 'e' },
        metadata: { version: 3 },
      });
      jest.spyOn(mockVault, 'updateKVSecret').mockResolvedValueOnce({ ok: true });

      const result = await vaultService.mergePvtKey({ newKey: 'v' }, name, path);

      expect(result).toEqual({ ok: true });
      expect(mockVault.updateKVSecret).toHaveBeenCalledWith(
        (vaultService as any).token,
        secretPath,
        { existingKey: 'e', newKey: 'v' },
        3,
      );
    });

    it('does NOT overwrite the secret on a non-404 vault error — fails closed instead', async () => {
      jest.spyOn(mockVault, 'readKVSecret').mockRejectedValueOnce(vaultError(503));
      const createSpy = jest.spyOn(mockVault, 'createKVSecret');
      const loggerErrorSpy = jest.spyOn(Logger, 'error').mockImplementation();

      await expect(
        vaultService.mergePvtKey({ newKey: 'v' }, name, path),
      ).rejects.toThrowError(InternalServerErrorException);

      expect(createSpy).not.toHaveBeenCalled();
      expect(loggerErrorSpy).toHaveBeenCalled();
    });

    it('does NOT overwrite the secret on a non-Vault error (e.g. network failure) — fails closed instead', async () => {
      jest.spyOn(mockVault, 'readKVSecret').mockRejectedValueOnce(new Error('ECONNRESET'));
      const createSpy = jest.spyOn(mockVault, 'createKVSecret');
      jest.spyOn(Logger, 'error').mockImplementation();

      await expect(
        vaultService.mergePvtKey({ newKey: 'v' }, name, path),
      ).rejects.toThrowError(InternalServerErrorException);

      expect(createSpy).not.toHaveBeenCalled();
    });
  });
});
