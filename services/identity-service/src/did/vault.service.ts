import { Injectable } from "@nestjs/common";
const Vault = require('hashi-vault-js');

@Injectable()
export class VaultService {
    private token: string;
    private vault: any;
    constructor() {
        console.log('process.env.VAULT_PROXY: ', process.env.VAULT_PROXY);
        this.token = process.env.VAULT_TOKEN;
        this.vault = new Vault({
            https: false,
            baseUrl: process.env.VAULT_BASE_URL,
            rootPath: process.env.VAULT_ROOT_PATH,
            timeout: process.env.VAULT_TIMEOUT,
            proxy: process.env.VAULT_PROXY === 'true' ? true : false,
        });
    }

    async checkStatus() {
        const status = await this.vault.healthCheck();
        const vault_config = await this.vault.readKVEngineConfig(this.token);
        console.log(vault_config);
        return status;
    }
    async writePvtKey(secret: string, name: string, path?: string) {
        const createSecret = await this.vault.createKVSecret(this.token, path ? path + `/${name}` : `ulp/identity-ms/private_keys/${name}`, secret)
        return createSecret;
    }

    async readPvtKey(name: string, path?: string) {
        const read = await this.vault.readKVSecret(this.token, path ? path + `/${name}` : `ulp/identity-ms/private_keys/${name}`)
        return read.data;
    }

}