import { Injectable } from "@nestjs/common";
const Vault = require('hashi-vault-js');



@Injectable()
export class VaultService {
    constructor () {}
    private token = process.env.VAULT_TOKEN;
    private vault = new Vault( {
            https: false,
            baseUrl: `${process.env.VAULT_ADDR}/v1`,
            rootPath: `${process.env.VAULT_ADDR}/v1/kv`,
            timeout: 5000,
            proxy: false
        });

    async checkStatus() {
        const status = await this.vault.healthCheck();
        const vault_config = await this. vault.readKVEngineConfig(this.token);
        console.log(vault_config);

        

        // const Item={
        //     name: "Test Secret",
        //     data: {
        //       bot_token1: "xoxb-123456789012-1234567890123-1w1lln0tt3llmys3cr3tatm3",
        //       bot_token2: "xoxb-123456789013-1234567890124-1w1lln0tt3llmys3cr3tatm3"
        //     }
        //   };
          
        // const data = await vault.createKVSecret(process.env.VAULT_TOKEN, Item.name , Item.data);

        return status//secretEngineInfo(process.env.VAULT_TOKEN);
    }
    async writePvtKey(secret: string, name: string, path?: string) {
        const createSecret = await this.vault.createKVSecret(this.token, path ? path + `/${name}`: `ulp/identity-ms/private_keys/${name}`, secret) 
        return createSecret;
    }

    async readPvtKey(name: string, path?: string) {
        const read = await this.vault.readKVSecret(this.token, path ? path + `/${name}`: `ulp/identity-ms/private_keys/${name}`)
        //console.log(read);
        return read.data;
    }

}