import { Injectable, Logger } from "@nestjs/common";
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
  async writePvtKey(secret: string, name: string, path?: string) {
    const createSecret = await this.vault.createKVSecret(
      this.token,
      path ? path + `/${name}` : `rcw/identity/private_keys/${name}`,
      secret
    );
    return createSecret;
  }

  async readPvtKey(name: string, path?: string) {
    const read = await this.vault.readKVSecret(
      this.token,
      path ? path + `/${name}` : `rcw/identity/private_keys/${name}`
    );
    return read.data;
  }
}
