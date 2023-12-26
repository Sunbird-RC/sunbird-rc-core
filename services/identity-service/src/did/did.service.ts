import { Injectable, InternalServerErrorException, Logger, NotFoundException, OnModuleInit } from '@nestjs/common';
import * as ION from '@decentralized-identity/ion-tools';
import { PrismaService } from '../utils/prisma.service';
import { DIDDocument, DIDResolutionResult, Resolver } from 'did-resolver';
import * as web from 'web-did-resolver';
import { uuid } from 'uuidv4';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
@Injectable()
export class DidService implements OnModuleInit {
  webDidResolver: Resolver;
  webDidPrefix: string;
  enableWebDid: boolean;
  constructor(private prisma: PrismaService, private vault: VaultService) {
    this.webDidPrefix = `did:web:${process.env.WEB_DID_ENDPOINT}:`;
    this.enableWebDid = process.env.ENABLE_WEB_DID === "true";
  }

  onModuleInit() {
    let resolver = web.getResolver();
    this.webDidResolver = new Resolver(resolver);
  }

  async generateDID(doc: GenerateDidDTO): Promise<DIDDocument> {
    // Create private/public key pair
    let authnKeys;
    try {
      authnKeys = await ION.generateKeyPair(process.env.SIGNING_ALGORITHM as string);
    } catch (err) {
      Logger.error(`Error generating key pair: ${err}`);
      throw new InternalServerErrorException('Error generating key pair');
    }

    let didUri: string;
    if(doc?.id && doc.id.startsWith("did:")) {
      // using id as did uri if provided
      didUri = doc.id;
    } else if(this.enableWebDid && doc?.method === 'web') {
      didUri = `${this.webDidPrefix}:${uuid()}`;
    } else {
      didUri = `${((doc?.method && doc.method?.trim() !== '') ? `did:${doc.method.trim()}:` : 'did:rcw:')}${uuid()}`;
    }

    // Create a DID Document
    const document: DIDDocument = {
      '@context': 'https://w3id.org/did/v1',
      id: didUri,
      alsoKnownAs: doc.alsoKnownAs,
      service: doc.services,
      verificationMethod: [
        {
          id: 'auth-key',
          type: process.env.SIGNING_ALGORITHM,
          publicKeyJwk: authnKeys.publicJwk,
          controller: didUri,
        },
      ],
      authentication: ['auth-key'],
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
      await this.vault.writePvtKey(authnKeys.privateJwk, didUri);
    } catch (err) {
      Logger.error(err);
      throw new InternalServerErrorException('Error writing private key to vault');
    }

    return document;
  }

  async resolveDID(id: string, resolveWebDid: boolean): Promise<DIDDocument> {
    let artifact: Identity;
    if(this.enableWebDid && resolveWebDid) {
      id = `${this.webDidPrefix}${id}`;
    }
    try {
      if (!id.startsWith("did:web") || id.startsWith(this.webDidPrefix)) {
        artifact = await this.prisma.identity.findUnique({
          where: { id },
        });
      }
    } catch (err) {
      Logger.error(`Error fetching DID: ${id} from db, ${err}`);
      throw new InternalServerErrorException(`Error fetching DID: ${id} from db`);
    }

    if(id.startsWith("did:web:") && !id.startsWith(this.webDidPrefix)) {
      console.debug("checking for web did: ", id);
      const result: DIDResolutionResult = await this.webDidResolver.resolve(id);
      console.debug("Fetch did result: ", result);
      if(result?.didDocument != null) {
        return result.didDocument;
      }
    }

    if (artifact) {
      return JSON.parse(artifact.didDoc as string) as DIDDocument;
    } else {
      throw new NotFoundException(`DID: ${id} not found`);
    }
  }
}
