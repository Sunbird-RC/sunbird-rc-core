import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import * as ION from '@decentralized-identity/ion-tools';
import { PrismaService } from '../utils/prisma.service';
import { DIDDocument } from 'did-resolver';
import { uuid } from 'uuidv4';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
@Injectable()
export class DidService {
  static getKeySignType = (algo?: string): any => {
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

  webDidBaseUrl: string;
  signingAlgorithm: string;
  constructor(private prisma: PrismaService, private vault: VaultService) {
    let baseUrl: string = process.env.WEB_DID_BASE_URL;
    if(baseUrl && typeof baseUrl === "string") {
      baseUrl = baseUrl.replace("https://", "")
      .replace(/:/g, "%3A")
      .replace(/\//g, ":");
      this.webDidBaseUrl = `did:web:${baseUrl}:`;
    }
    this.signingAlgorithm = process.env.SIGNING_ALGORITHM;
  }

  generateDidUri(method: string): string {
    if (method === 'web') {
      return this.getWebDidIdForId(uuid());
    }
    return `did:${(method && method.trim() !== '') ? method.trim() : 'rcw'}:${uuid()}`;
  }

  getWebDidIdForId(id: string): string {
    if(!this.webDidBaseUrl) throw new NotFoundException("Web did base url not found");
    return `${this.webDidBaseUrl}${id}`;
  }

  async generateKeyPairs(num: number = 1): Promise<[{ publicJwk: string, privateJwk: string }]> {
    try {
      return await Promise.all(
        Array.from(Array(1)).map(() => ION.generateKeyPair(this.signingAlgorithm))
      );
    } catch (err: any) {
      Logger.error(`Error generating key pair: ${err}`);
      throw new InternalServerErrorException('Error generating key pair');
    }
  }

  getKeyType = () => {
    return DidService.getKeySignType(this.signingAlgorithm)?.keyType
  }

  async generateDID(doc: GenerateDidDTO): Promise<DIDDocument> {
    // Create private/public key pair
    let keyPairs = await this.generateKeyPairs();

    const didUri: string = this.generateDidUri(doc?.method);

    const keytype = this.getKeyType();

    let verificationMethodsWithPvtKeys = keyPairs.map((d, index) => ({
      id: `${didUri}#key-${index}`,
      type: keytype,
      publicKeyJwk: d.publicJwk,
      privateJwk: d.privateJwk,
      controller: didUri,
    }));
    let verificationMethods = verificationMethodsWithPvtKeys.map(({privateJwk, ...d}) => d);
    let verificationMethodIds = verificationMethods.map(d => d.id);

    // Create a DID Document
    const document: DIDDocument = {
      '@context': [
        "https://www.w3.org/ns/did/v1",
        "https://w3id.org/security/suites/jws-2020/v1",
        // "https://w3id.org/security/suites/ed25519-2020/v1"
      ],
      id: didUri,
      alsoKnownAs: doc.alsoKnownAs,
      service: doc.services,
      verificationMethod: (verificationMethods as any),
      authentication: verificationMethodIds,
      assertionMethod: verificationMethodIds
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

    await Promise.all(verificationMethodsWithPvtKeys.map(d => this.vault
      .writePvtKey(d.privateJwk, d.id)));

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
