export type SecretProviderType = 'vault' | 'aws';

export interface SecretsHealthResult {
  healthy: boolean;
  provider: SecretProviderType;
  details?: Record<string, unknown>;
}

export function resolveSecretProviderType(): SecretProviderType {
  const provider = (process.env.SECRET_PROVIDER || 'vault').toLowerCase();
  if (provider === 'aws') {
    return 'aws';
  }
  return 'vault';
}

export function buildPrivateKeyPath(name: string, path?: string): string {
  return path ? `${path}/${name}` : `rcw/identity/private_keys/${name}`;
}

/** One AWS secret per DID; colons in the DID become slashes in the secret name. */
export function toAwsSecretName(name: string, path?: string): string {
  const secretPath = buildPrivateKeyPath(name, path).replace(/:/g, '/');
  const prefix = process.env.AWS_SECRETS_PREFIX;
  return prefix ? `${prefix}/${secretPath}` : secretPath;
}

export abstract class SecretsProvider {
  abstract writePvtKey(secret: object, name: string, path?: string): Promise<unknown>;
  abstract readPvtKey(name: string, path?: string): Promise<Record<string, unknown> | undefined>;
  abstract checkStatus(): Promise<SecretsHealthResult>;
}
