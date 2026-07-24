import { Injectable, InternalServerErrorException, Logger } from "@nestjs/common";
const Vault = require("hashi-vault-js");

@Injectable()
export class VaultService {
  private token: string;
  private vault: any;
  constructor() {
    this.token = process.env.VAULT_TOKEN;
    this.vault = new Vault({
      https: false,
      baseUrl: process.env.VAULT_BASE_URL,
      rootPath: process.env.VAULT_ROOT_PATH,
      timeout: process.env.VAULT_TIMEOUT,
      proxy: process.env.VAULT_PROXY === "true" ? true : false,
    });
  }

  async checkStatus() {
    let status, vault_config;
    try {
      status = await this.vault.healthCheck();
    } catch (err) {
      Logger.error(`Error in checking vault status: ${err}`);
    }
    try {
      vault_config = await this.vault.readKVEngineConfig(this.token);
    } catch (err) {
      Logger.error(`Error in checking vault config: ${err}`);
    }
    return { status, vault_config };
  }
  async writePvtKey(secret: object, name: string, path?: string) {
    try {
      const createSecret = await this.vault.createKVSecret(
        this.token,
        path ? path + `/${name}` : `rcw/identity/private_keys/${name}`,
        secret
      );
      return createSecret;
    } catch (err) {
      Logger.error(err);
      throw new InternalServerErrorException('Error writing private key to vault');
    }
  }

  async readPvtKey(name: string, path?: string) {
    const read = await this.vault.readKVSecret(
      this.token,
      path ? path + `/${name}` : `rcw/identity/private_keys/${name}`
    );
    return read.data;
  }

  // Merges new key entries into an existing secret (or creates it).
  // Used when adding an ES256/JWT key to a DID that already holds an Ed25519 key.
  async mergePvtKey(secret: object, name: string, path?: string) {
    const secretPath = path
      ? path + `/${name}`
      : `rcw/identity/private_keys/${name}`;
    let existing: any = null;
    let version: number | undefined;
    try {
      const read = await this.vault.readKVSecret(this.token, secretPath);
      existing = read?.data;
      version = read?.metadata?.version ?? read?.version;
    } catch (err) {
      // secret does not exist yet — fall through to create
    }
    try {
      const merged = { ...(existing || {}), ...secret };
      if (existing) {
        return await this.vault.updateKVSecret(this.token, secretPath, merged, version);
      }
      return await this.vault.createKVSecret(this.token, secretPath, merged);
    } catch (err) {
      Logger.error(err);
      throw new InternalServerErrorException('Error merging private key into vault');
    }
  }
}
