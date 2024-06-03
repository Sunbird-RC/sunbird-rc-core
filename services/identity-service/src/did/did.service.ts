import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../utils/prisma.service';
const { DIDDocument } = require('did-resolver');
type DIDDocument = typeof DIDDocument;
import { v4 as uuid } from 'uuid';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
import { RSAKeyPair } from "crypto-ld";
type KeysType = {
  [key: string]: {
    name: string;
    key: any;
  };
};
@Injectable()
export class DidService {
  keys: KeysType = {
  }
  webDidBaseUrl: string;
  signingAlgorithm: string;
  didResolver: any;
  constructor(private prisma: PrismaService, private vault: VaultService) {
    let baseUrl: string = process.env.WEB_DID_BASE_URL;
    if(baseUrl && typeof baseUrl === "string") {
      baseUrl = baseUrl.replace("https://", "")
      .replace(/:/g, "%3A")
      .replace(/\//g, ":");
      this.webDidBaseUrl = `did:web:${baseUrl}:`;
    }
    this.signingAlgorithm = process.env.SIGNING_ALGORITHM;
    this.init();
  }

  async init() {
    const {Ed25519VerificationKey2020} = await import('@digitalbazaar/ed25519-verification-key-2020');
    const {Ed25519VerificationKey2018} = await import('@digitalbazaar/ed25519-verification-key-2018');
    this.keys['Ed25519Signature2020'] = {
      name: 'Ed25519VerificationKey2020',
      key: Ed25519VerificationKey2020
    };
    this.keys['Ed25519Signature2018'] = {
      name: 'Ed25519VerificationKey2018',
      key: Ed25519VerificationKey2018
    };
    this.keys['RsaSignature2018'] = {
      name: 'RsaVerificationKey2018',
      key: RSAKeyPair
    };
    const { Resolver } = await import('did-resolver');
    const { getResolver } = await import('web-did-resolver');
    const webResolver = getResolver();
    this.didResolver = new Resolver({
      ...webResolver
        //...you can flatten multiple resolver methods into the Resolver
    });
  }

  generateDidUri(method: string, id: string): string {
    if(id) return id;
    if (method === 'web') {
      return this.getWebDidIdForId(uuid());
    }
    return `did:${(method && method.trim() !== '') ? method.trim() : 'rcw'}:${uuid()}`;
  }

  getWebDidIdForId(id: string): string {
    if(!this.webDidBaseUrl) throw new NotFoundException("Web did base url not found");
    return `${this.webDidBaseUrl}${id}`;
  }

  async getVerificationKey(signingAlgorithm?: string): Promise<any> {
    if(!this.keys[signingAlgorithm]) {
      await this.init();
    }
    if(!this.keys[signingAlgorithm]) {
      throw new NotFoundException("Signature suite not supported")
    }
    return this.keys[signingAlgorithm];
  }

  async getVerificationKeyByName(name?: string) {
    let verificationKey = Object.values(this.keys).find((d: {name: string, key: any}) => d.name === name);
    if(!verificationKey) {
      await this.init();
      verificationKey = Object.values(this.keys).find((d: {name: string, key: any}) => d.name === name);
    }
    if(name && !verificationKey) throw new NotFoundException("Verification Key '" + name + "' not found");
    return verificationKey;
  }

  async generateDID(doc: GenerateDidDTO): Promise<DIDDocument> {
    // Create a UUID for the DID using uuidv4
    const didUri: string = this.generateDidUri(doc?.method, doc?.id);

    // Create private/public key pair
    let authnKeys;
    let privateKeys: object;
    let verificationKey =  await this.getVerificationKeyByName(doc?.keyPairType);
    if(!verificationKey) verificationKey = await this.getVerificationKey(this.signingAlgorithm);
    try {
      const keyPair = await (verificationKey as any)?.key.generate({
        id: `${didUri}#key-0`,
        controller: didUri
      });
      const exportedKey = await keyPair.export({
        publicKey: true, privateKey: true, includeContext: true
      });
      let privateKey = {};
      if(verificationKey?.name === "Ed25519VerificationKey2020") {
        const {privateKeyMultibase, ...rest } = exportedKey;
        authnKeys = rest;
        privateKey = {privateKeyMultibase};
      } else if(verificationKey?.name === "Ed25519VerificationKey2018") {
        const {privateKeyBase58, ...rest } = exportedKey;
        authnKeys = rest;
        privateKey = {privateKeyBase58};
      } else if(verificationKey?.name === "RsaVerificationKey2018") {
        const {privateKeyPem, ...rest } = exportedKey;
        authnKeys = {...rest};
        privateKey = {privateKeyPem};
      } else {
        throw new NotFoundException("VerificationKey type not found");
      }
      privateKeys = {
        [authnKeys.id]: privateKey
      };
    } catch (err: any) {
            Logger.error(`Error generating key pair: ${err}`);
      throw new InternalServerErrorException('Error generating key pair: ' + err.message);
    }

    const keyId = authnKeys?.id;

    // Create a DID Document
    const document: DIDDocument = {
      '@context': [
        "https://www.w3.org/ns/did/v1"
      ],
      id: didUri,
      alsoKnownAs: doc.alsoKnownAs,
      service: doc.services,
      verificationMethod: [
        authnKeys,
      ],
      authentication: [keyId],
      assertionMethod: [keyId]
    };

    try {
      await this.prisma.identity.create({
        data: {
          id: didUri,
          didDoc: JSON.stringify(document),
        },
      });
    } catch (err) {
      Logger.error(`Error writing DID to database ${err}`);
      throw new InternalServerErrorException('Error writing DID to database');
    }

    try {
      await this.vault.writePvtKey(privateKeys, didUri);
    } catch (err) {
      Logger.error(err);
      throw new InternalServerErrorException('Error writing private key to vault');
    }

    return document;
  }

  async resolveDID(id: string): Promise<DIDDocument> {
    let artifact: Identity;
    try {
      artifact = await this.prisma.identity.findUnique({
        where: { id: id?.split("#")[0] },
      });
    } catch (err) {
      Logger.error(`Error fetching DID: ${id} from db, ${err}`);
      throw new InternalServerErrorException(`Error fetching DID: ${id} from db`);
    }

    if(!artifact && id?.startsWith("did:web") && !id?.startsWith(this.webDidBaseUrl)) {
      try {
        return (await this.didResolver.resolve(id)).didDocument;
      } catch (err) {
        Logger.error(`Error fetching DID: ${id} from web, ${err}`);
        throw new InternalServerErrorException(`Error fetching DID: ${id} from web`);
      }
    }

    if (artifact) {
      return JSON.parse(artifact.didDoc as string) as DIDDocument;
    } else {
      throw new NotFoundException(`DID: ${id} not found`);
    }
  }

  async resolveWebDID(id: string): Promise<DIDDocument> {
    const webDidId = this.getWebDidIdForId(id);
    return this.resolveDID(webDidId);
  }
}
