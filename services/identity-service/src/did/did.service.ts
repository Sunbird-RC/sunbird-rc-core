import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import * as ION from '@decentralized-identity/ion-tools';
import { PrismaService } from 'src/prisma.service';
import { DIDDocument } from 'did-resolver';
import { uuid } from 'uuidv4';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { VaultService } from './vault.service';
import { Identity } from '@prisma/client';
@Injectable()
export class DidService {
  constructor(private prisma: PrismaService, private vault: VaultService) {}

  async generateDID(doc: GenerateDidDTO): Promise<DIDDocument> {
    // Create private/public key pair
    let authnKeys;
    try {
      authnKeys = await ION.generateKeyPair(process.env.SIGNING_ALGORITHM as string);
    } catch (err) {
      Logger.error(`Error generating key pair`);
      throw new InternalServerErrorException('Error generating key pair');
    }

    // Create a UUID for the DID using uuidv4
    const didUri = `did:${(doc.method && doc.method.trim() !== '') ? doc.method.trim() : 'rcw'}:${uuid()}`;

    // Create a DID Document
    const document: DIDDocument = {
      '@context': 'https://w3id.org/did/v1',
      id: didUri,
      alsoKnownAs: doc.alsoKnownAs,
      service: doc.service,
      verificationMethod: [
        {
          id: 'auth-key',
          type: 'Ed25519VerificationKey2020',
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
      Logger.error(`Error writing DID to database`);
      throw new InternalServerErrorException('Error writing DID to database');
    }

    try {
      await this.vault.writePvtKey(authnKeys.privateJwk, didUri);
    } catch (err) {
      Logger.error(`Error saving private keys to vault`);
    }

    return document;
  }

  async resolveDID(id: string): Promise<DIDDocument> {
    let artifact: Identity;
    try {
      artifact = await this.prisma.identity.findUnique({
        where: { id },
      });
    } catch (err) {
      Logger.error(`Error fetching DID: ${id} from db`);
      throw new InternalServerErrorException(`Error fetching DID: ${id} from db`);
    }

    if (artifact) {
      return JSON.parse(artifact.didDoc as string) as DIDDocument;
    } else {
      throw new NotFoundException(`DID: ${id} not found`);
    }
  }
}
