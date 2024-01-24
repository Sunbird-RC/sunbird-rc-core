import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import { generateKeyPair } from '@decentralized-identity/ion-tools';
import { PrismaService } from '../utils/prisma.service';
import { DIDDocument } from 'did-resolver';
import { uuid } from 'uuidv4';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
@Injectable()
export class DidService {
  static getKeySignType = (algo: string): any => {
    switch (algo) {
      case 'Ed25519':
      case 'EdDSA':
        return {
          keyType: "JsonWebKey2020",
          signType: "JsonWebSignature2020"
        };
      case 'secp256k1':
      case 'ES256K':
        return {
          keyType: "EcdsaSecp256k1VerificationKey2019",
          signType: "EcdsaSecp256k1Signature2019"
        };
      default:
        return {};
    }
  }

  constructor(private prisma: PrismaService, private vault: VaultService) {}

  async generateDID(doc: GenerateDidDTO): Promise<DIDDocument> {
    // Create private/public key pair
    let authnKeys;
    let signingAlgorithm: string = process.env.SIGNING_ALGORITHM;
    try {
      authnKeys = await generateKeyPair(signingAlgorithm);
    } catch (err: any) {
      Logger.error(`Error generating key pair: ${err}`);
      throw new InternalServerErrorException('Error generating key pair');
    }

    // Create a UUID for the DID using uuidv4
    const didUri = `did:${(doc.method && doc.method.trim() !== '') ? doc.method.trim() : 'rcw'}:${uuid()}`;

    const keyId = `${didUri}#key-0`;

    // Create a DID Document
    const document: DIDDocument = {
      '@context': 'https://w3id.org/did/v1',
      id: didUri,
      alsoKnownAs: doc.alsoKnownAs,
      service: doc.services,
      verificationMethod: [
        {
          id: keyId,
          type: DidService.getKeySignType(signingAlgorithm)?.keyType,
          publicKeyJwk: authnKeys.publicJwk,
          controller: didUri,
        },
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
      await this.vault.writePvtKey(authnKeys.privateJwk, didUri);
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
        where: { id },
      });
    } catch (err) {
      Logger.error(`Error fetching DID: ${id} from db, ${err}`);
      throw new InternalServerErrorException(`Error fetching DID: ${id} from db`);
    }

    if (artifact) {
      return JSON.parse(artifact.didDoc as string) as DIDDocument;
    } else {
      throw new NotFoundException(`DID: ${id} not found`);
    }
  }
}
