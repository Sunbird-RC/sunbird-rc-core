import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../utils/prisma.service';
import { v4 as uuid } from 'uuid';
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
import { RSAKeyPair } from 'crypto-ld';
import { GenerateDidDTO } from './dtos/GenerateDidRequest.dto';

const { DIDDocument } = require('did-resolver');
type DIDDocument = typeof DIDDocument;

@Injectable()
export class DidService {
  keys = {}
  webDidPrefix: string;
  signingAlgorithm: string;
  didResolver: any;
  constructor(private prisma: PrismaService, private vault: VaultService) {
    let baseUrl: string = process.env.WEB_DID_BASE_URL;
    this.webDidPrefix = this.getDidPrefixForBaseUrl(baseUrl);
    this.signingAlgorithm = process.env.SIGNING_ALGORITHM;
    this.init();
  }

  async init() {
    const {Ed25519VerificationKey2020} = await import('@digitalbazaar/ed25519-verification-key-2020');
    const {Ed25519VerificationKey2018} = await import('@digitalbazaar/ed25519-verification-key-2018');
    this.keys['Ed25519Signature2020'] = Ed25519VerificationKey2020;
    this.keys['Ed25519Signature2018'] = Ed25519VerificationKey2018;
    this.keys['RsaSignature2018'] = RSAKeyPair;
    const { Resolver } = await import('did-resolver');
    const { getResolver } = await import('web-did-resolver');
    const webResolver = getResolver();
    this.didResolver = new Resolver({
      ...webResolver
        //...you can flatten multiple resolver methods into the Resolver
    });
  }

  getDidPrefixForBaseUrl(baseUrl: string): string {
    if(!baseUrl || typeof baseUrl !== "string") return '';
    baseUrl = baseUrl.split("?")[0]
      .replace("https://", "")
      .replace(/:/g, "%3A")
      .replace(/\//g, ":");
    return `did:web:${baseUrl}:`;
  }

  generateDidUri(method: string, id?: string, webDidBaseUrl?: string): string {
    if(id) return id;
    if (method === 'web') {
      return this.getWebDidIdForId(uuid(), webDidBaseUrl);
    }
    return `did:${(method && method.trim() !== '') ? method.trim() : 'rcw'}:${uuid()}`;
  }

  getWebDidIdForId(id: string, webDidBaseUrl?: string): string {
    if(!this.webDidPrefix && !webDidBaseUrl) throw new NotFoundException("Web did base url not found");
    if(webDidBaseUrl) {
      const webDidBasePrefix = this.getDidPrefixForBaseUrl(webDidBaseUrl);
      return `${webDidBasePrefix}${id}`;
    }
    return `${this.webDidPrefix}${id}`;
  }

  async generateDID(doc: GenerateDidDTO): Promise<DIDDocument> {
    // Create a UUID for the DID using uuidv4
    const didUri: string = this.generateDidUri(doc?.method, doc?.id, doc?.webDidBaseUrl);

    // Create private/public key pair
    let authnKeys;
    let privateKeys: object;
    let signingAlgorithm: string = this.signingAlgorithm;
    try {
      if(!this.keys[signingAlgorithm]) {
        await this.init();
      }
      if(!this.keys[signingAlgorithm]) {
        throw new NotFoundException("Signature suite not supported")
      }
      const keyPair = await this.keys[signingAlgorithm].generate({
        id: `${didUri}#key-0`,
        controller: didUri
      });
      const exportedKey = await keyPair.export({
        publicKey: true, privateKey: true, includeContext: true
      });
      let privateKey = {};
      if(signingAlgorithm === "Ed25519Signature2020") {
        const {privateKeyMultibase, ...rest } = exportedKey;
        authnKeys = rest;
        privateKey = {privateKeyMultibase};
      } else if(signingAlgorithm === "Ed25519Signature2018") {
        const {privateKeyBase58, ...rest } = exportedKey;
        authnKeys = rest;
        privateKey = {privateKeyBase58};
      } else if(signingAlgorithm === "RsaSignature2018") {
        const {privateKeyPem, ...rest } = exportedKey;
        authnKeys = {...rest};
        privateKey = {privateKeyPem};
      } else {
        throw new NotFoundException("Signature type not found");
      }
      privateKeys = {
        [authnKeys.id]: privateKey
      };
    } catch (err: any) {
            Logger.error(`Error generating key pair: ${err}`);
      throw new InternalServerErrorException('Error generating key pair');
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

    if(!artifact && id?.startsWith("did:web") && !id?.startsWith(this.webDidPrefix)) {
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
