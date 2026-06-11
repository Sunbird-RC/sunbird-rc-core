import { Global, Module } from '@nestjs/common';
import { VaultService } from '../utils/vault.service';
import { AwsSecretsProvider } from './aws-secrets.provider';
import {
  resolveSecretProviderType,
  SecretsProvider,
} from './secrets-provider';
import { VaultSecretsProvider } from './vault-secrets.provider';

@Global()
@Module({
  providers: [
    VaultService,
    VaultSecretsProvider,
    AwsSecretsProvider,
    {
      provide: SecretsProvider,
      useFactory: (
        vault: VaultSecretsProvider,
        aws: AwsSecretsProvider,
      ): SecretsProvider => {
        return resolveSecretProviderType() === 'aws' ? aws : vault;
      },
      inject: [VaultSecretsProvider, AwsSecretsProvider],
    },
  ],
  exports: [SecretsProvider],
})
export class SecretsModule {}
