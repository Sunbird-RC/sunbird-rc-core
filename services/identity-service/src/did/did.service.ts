import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../utils/prisma.service';
const { DIDDocument } = require('did-resolver');
type DIDDocument = typeof DIDDocument;
import { v4 as uuid } from 'uuid';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
import { RSAKeyPair } from "crypto-ld";
@Injectable()
export class DidService {
  keys = {}
  static getKeySignType = (algo: string): any => {
    return {
      keyType: "JsonWebKey2020",
      signType: "JsonWebSignature2020"
    };
    // switch (algo) {
    //   case 'Ed25519':
    //   case 'EdDSA':
    //     return {
    //       keyType: "JsonWebKey2020",
    //       signType: "JsonWebSignature2020"
    //     };
    //   case 'secp256k1':
    //   case 'ES256K':
    //     return {
    //       keyType: "EcdsaSecp256k1VerificationKey2019",
    //       signType: "EcdsaSecp256k1Signature2019"
    //     };
    //   default:
    //     return {};
    // }
  }

  constructor(private prisma: PrismaService, private vault: VaultService) {
    this.init();
  }

  async init() {
    const {Ed25519VerificationKey2020} = await import('@digitalbazaar/ed25519-verification-key-2020');
    const {Ed25519VerificationKey2018} = await import('@digitalbazaar/ed25519-verification-key-2018');
    this.keys['Ed25519Signature2020'] = Ed25519VerificationKey2020;
    this.keys['Ed25519Signature2018'] = Ed25519VerificationKey2018;
    this.keys['RsaSignature2018'] = RSAKeyPair;
  }

  async generateDID(doc: GenerateDidDTO): Promise<DIDDocument> {
    // Create a UUID for the DID using uuidv4
    const didUri = `did:${(doc.method && doc.method.trim() !== '') ? doc.method.trim() : 'rcw'}:${uuid()}`;

    // Create private/public key pair
    let authnKeys;
    let privateKeys: object;
    let signingAlgorithm: string = process.env.SIGNING_ALGORITHM;
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
