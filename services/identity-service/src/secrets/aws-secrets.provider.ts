import {
  CreateSecretCommand,
  GetSecretValueCommand,
  PutSecretValueCommand,
  ResourceExistsException,
  SecretsManagerClient,
} from '@aws-sdk/client-secrets-manager';
import { Injectable, InternalServerErrorException, Logger } from '@nestjs/common';
import {
  SecretsHealthResult,
  SecretsProvider,
  toAwsSecretName,
} from './secrets-provider';

@Injectable()
export class AwsSecretsProvider extends SecretsProvider {
  private readonly client: SecretsManagerClient;

  constructor() {
    super();
    const region = process.env.AWS_REGION || process.env.AWS_DEFAULT_REGION;
    if (!region) {
      Logger.warn(
        'AWS_REGION is not set; AWS Secrets Manager client may fail to connect',
      );
    }
    this.client = new SecretsManagerClient({ region });
  }

  async checkStatus(): Promise<SecretsHealthResult> {
    const probeSecret =
      process.env.AWS_SECRETS_PROBE_SECRET ||
      (process.env.AWS_SECRETS_PREFIX
        ? `${process.env.AWS_SECRETS_PREFIX}/rcw/identity/health`
        : 'rcw/identity/health');
    try {
      await this.client.send(
        new GetSecretValueCommand({ SecretId: probeSecret }),
      );
      return { healthy: true, provider: 'aws' };
    } catch (err: any) {
      if (err?.name === 'ResourceNotFoundException') {
        return {
          healthy: true,
          provider: 'aws',
          details: {
            message:
              'AWS Secrets Manager reachable (probe secret not found, which is expected)',
          },
        };
      }
      Logger.error(`Error in checking AWS Secrets Manager status: ${err}`);
      return { healthy: false, provider: 'aws', details: { error: String(err) } };
    }
  }

  async writePvtKey(secret: object, name: string, path?: string) {
    const secretName = toAwsSecretName(name, path);
    const secretString = JSON.stringify(secret);
    Logger.log(`Writing private key to AWS secret: ${secretName}`);
    try {
      return await this.client.send(
        new CreateSecretCommand({
          Name: secretName,
          SecretString: secretString,
        }),
      );
    } catch (err) {
      if (err instanceof ResourceExistsException) {
        try {
          return await this.client.send(
            new PutSecretValueCommand({
              SecretId: secretName,
              SecretString: secretString,
            }),
          );
        } catch (updateErr) {
          Logger.error(updateErr);
          throw new InternalServerErrorException(
            'Error updating private key in AWS Secrets Manager',
          );
        }
      }
      Logger.error(err);
      throw new InternalServerErrorException(
        'Error writing private key to AWS Secrets Manager',
      );
    }
  }

  async readPvtKey(name: string, path?: string) {
    const secretName = toAwsSecretName(name, path);
    try {
      const response = await this.client.send(
        new GetSecretValueCommand({ SecretId: secretName }),
      );
      if (!response.SecretString) {
        return undefined;
      }
      return JSON.parse(response.SecretString);
    } catch (err: any) {
      if (err?.name === 'ResourceNotFoundException') {
        return undefined;
      }
      Logger.error(err);
      throw new InternalServerErrorException(
        'Error reading private key from AWS Secrets Manager',
      );
    }
  }
}
