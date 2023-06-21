import { Injectable } from '@nestjs/common';
import * as ION from '@decentralized-identity/ion-tools';
import { PrismaService } from 'src/prisma.service';
import { DIDDocument } from 'did-resolver';
import { uuid } from 'uuidv4';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { VaultService } from './vault.service';

@Injectable()
export class DidService {
  constructor(private prisma: PrismaService, private vault: VaultService) {}

  async generateDID(doc: GenerateDidDTO): Promise<DIDDocument> {
    // Create private/public key pair
    const authnKeys = await ION.generateKeyPair('Ed25519');

    // Create a UUID for the DID using uuidv4
    const didUri = `did:${doc.method}:${uuid()}`;

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

    await this.prisma.identity.create({
      data: {
        id: didUri,
        didDoc: JSON.stringify(document),
      },
    });
    this.vault.writePvtKey(authnKeys.privateJwk, didUri);
    return document;
  }

  async resolveDID(id: string): Promise<DIDDocument> {
    const artifact = await this.prisma.identity.findUnique({
      where: { id },
    });
    if (artifact) {
      return JSON.parse(artifact.didDoc as string) as DIDDocument;
    } else {
      return null;
    }
  }
}
