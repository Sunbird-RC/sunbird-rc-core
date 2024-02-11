import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../utils/prisma.service';
import { DidService } from '../did/did.service';
const { DIDDocument } = require('did-resolver');
type DIDDocument = typeof DIDDocument;
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
import * as jsigs from 'jsonld-signatures';
import * as jsonld from 'jsonld';
import { DOCUMENTS } from './documents';
const AssertionProofPurpose = jsigs.purposes.AssertionProofPurpose;
@Injectable()
export default class VcService {
  map = {
    Ed25519VerificationKey2020: null,
    Ed25519Signature2020: null
  };
  signTypeForKey = {
    Ed25519VerificationKey2020: "Ed25519Signature2020",
    JsonWebKey2020: "Ed25519Signature2020",
    Ed25519VerificationKey2018: "Ed25519Signature2020"
  }
  documents: object
  constructor(
    private readonly primsa: PrismaService,
    private readonly didService: DidService,
    private readonly vault: VaultService,
  ) {
    this.init();
  }

  async init() {
    const {Ed25519VerificationKey2020} = await import('@digitalbazaar/ed25519-verification-key-2020');
    const {Ed25519Signature2020} = await import('@digitalbazaar/ed25519-signature-2020');
    this.map.Ed25519VerificationKey2020 = Ed25519VerificationKey2020;
    this.map.Ed25519Signature2020 = Ed25519Signature2020;
  }

  async sign(signerDID: string, toSign: object) {
    let did: Identity;
    try {
      did = await this.primsa.identity.findUnique({
        where: { id: signerDID },
      });
    } catch (err) {
      Logger.error('Error fetching signerDID:', err);
      throw new InternalServerErrorException(`Error fetching signerDID`);
    }

    if (!did) throw new NotFoundException('Signer DID not found!');

    try {
      const didDoc = (JSON.parse(did.didDoc as string) as DIDDocument);
      const verificationMethod = didDoc.verificationMethod[0];
      const suite = await this.getSuite(verificationMethod, this.signTypeForKey[verificationMethod?.type], true);
      const signedVC = await jsigs.sign(toSign, {
          purpose: new AssertionProofPurpose(),
          suite: suite,
          documentLoader: this.getDocumentLoader(didDoc),
          addSuiteContext: true
        });
      return signedVC;
    } catch (err) {
      console.log(JSON.stringify(err, null, 2));
      Logger.error('Error signign the document:', err);
      throw new InternalServerErrorException(`Error signign the document`);
    }
  }

  async verify(signerDID: string, signedDoc: any): Promise<boolean> {
    let didDocument: DIDDocument;
    try {
      didDocument = await this.didService.resolveDID(signerDID);
    } catch (err) {
      Logger.error(`Error resolving signer did: `, err);
      throw new InternalServerErrorException(`Error resolving signer did`);
    }

    try {
      const verificationMethod = didDocument.verificationMethod[0];
      const suite = await this.getSuite(verificationMethod, signedDoc?.proof?.type);
      const results = await jsigs.verify(signedDoc, {
        purpose: new AssertionProofPurpose(),
        suite: [suite],
        documentLoader: this.getDocumentLoader(didDocument),
      });
      return !!results?.verified;
    } catch (e) {
      Logger.error(e);
      return false;
    }
  }

  async getSuite(verificationMethod, signatureType: string, withPrivateKey = false) {
    if(signatureType !== "Ed25519Signature2020") {
      throw new NotFoundException("Suite for signature type not found");
    }
    const supportedMethods = ["Ed25519VerificationKey2020", "JsonWebKey2020", "Ed25519VerificationKey2018"];
    if(!supportedMethods.includes(verificationMethod?.type)) {
      throw new NotFoundException("Suite for verification type not found");
    }
    const keyPair = await this.map.Ed25519VerificationKey2020.from(verificationMethod);
    if(withPrivateKey) {
      const privateKeys = await this.vault.readPvtKey(verificationMethod.id.split("#")[0]) || {};
      keyPair.privateKeyMultibase = privateKeys[verificationMethod.id];
    }
    return new this.map.Ed25519Signature2020({key: keyPair});
  }

  getDocumentLoader(didDoc: DIDDocument) {
    return jsigs.extendContextLoader(async url => {
      if(url === didDoc?.id) {
        return {
          contextUrl: null,
          documentUrl: url,
          document: didDoc
        };
      }
      if(DOCUMENTS[url]) {
        return {
          contextUrl: null,
          documentUrl: url,
          document: DOCUMENTS[url]
        }
      }
      return await jsonld.documentLoaders.node()(url);
    })
  }
}
