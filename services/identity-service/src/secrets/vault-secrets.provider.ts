import { Injectable } from '@nestjs/common';
import { VaultService } from '../utils/vault.service';
import { SecretsHealthResult, SecretsProvider } from './secrets-provider';

@Injectable()
export class VaultSecretsProvider extends SecretsProvider {
  constructor(private readonly vault: VaultService) {
    super();
  }

  async checkStatus(): Promise<SecretsHealthResult> {
    const { status } = await this.vault.checkStatus();
    const healthy =
      status?.initialized === true && status?.sealed === false;
    return {
      healthy,
      provider: 'vault',
      details: { status },
    };
  }

  writePvtKey(secret: object, name: string, path?: string) {
    return this.vault.writePvtKey(secret, name, path);
  }

  readPvtKey(name: string, path?: string) {
    return this.vault.readPvtKey(name, path);
  }
}
