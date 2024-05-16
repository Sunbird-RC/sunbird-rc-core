import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../utils/prisma.service';
import { DidService } from '../did/did.service';
const { DIDDocument } = require('did-resolver');
type DIDDocument = typeof DIDDocument;
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
import * as jsigs from '@digitalcredentials/jsonld-signatures';
import * as jsigs2 from 'jsonld-signatures';
import * as jsonld from 'jsonld';
import { DOCUMENTS } from './documents';
import { RSAKeyPair } from "crypto-ld";
const AssertionProofPurpose = jsigs.purposes.AssertionProofPurpose;
@Injectable()
export default class VcService {
  map = {
    Ed25519VerificationKey2020: null,
    JsonWebKey2020: null,
    Ed25519Signature2020: null,
    Ed25519VerificationKey2018: null,
    Ed25519Signature2018: null,
    RsaVerificationKey2018: null,
    RsaSignature2018: null
  };
  signTypeForKey = {
    Ed25519VerificationKey2020: "Ed25519Signature2020",
    JsonWebKey2020: "Ed25519Signature2020",
    Ed25519VerificationKey2018: "Ed25519Signature2018",
    RsaVerificationKey2018: "RsaSignature2018"
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
    const {Ed25519VerificationKey2018} = await import('@digitalbazaar/ed25519-verification-key-2018');
    const {Ed25519Signature2018} = await import('@digitalbazaar/ed25519-signature-2018');
    this.map.Ed25519VerificationKey2020 = Ed25519VerificationKey2020;
    this.map.JsonWebKey2020 = Ed25519VerificationKey2020;
    this.map.Ed25519Signature2020 = Ed25519Signature2020;
    this.map.Ed25519VerificationKey2018 = Ed25519VerificationKey2018;
    this.map.Ed25519Signature2018 = Ed25519Signature2018;
    this.map.RsaSignature2018 = jsigs2.suites.RsaSignature2018;
    this.map.RsaVerificationKey2018 = RSAKeyPair;
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
      const signatures = verificationMethod.type === "RsaVerificationKey2018" ? jsigs2: jsigs;
      const signedVC = await signatures.sign({...toSign}, {
          purpose: new AssertionProofPurpose(),
          suite: suite,
          documentLoader: this.getDocumentLoader(didDoc),
          addSuiteContext: true,
          compactProof: false
        });
      return signedVC;
    } catch (err) {
      console.log("Log: ", JSON.stringify(err, null, 4));
      Logger.error('Error signing the document:', err);
      throw new InternalServerErrorException(`Error signing the document`);
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
      const signatures = verificationMethod.type === "RsaVerificationKey2018" ? jsigs2: jsigs;
      const results = await signatures.verify(signedDoc, {
        purpose: new AssertionProofPurpose(),
        suite: [suite],
        documentLoader: this.getDocumentLoader(didDocument),
        compactProof: false
      });
      return !!results?.verified;
    } catch (e) {
      Logger.error(e);
      return false;
    }
  }

  async getSuite(verificationMethod, signatureType: string, withPrivateKey = false) {
    const supportedSignatures = {
      "Ed25519Signature2020": ["Ed25519VerificationKey2020", "JsonWebKey2020", "Ed25519VerificationKey2018"],
      "Ed25519Signature2018": ["Ed25519VerificationKey2018"],
      "RsaSignature2018": ["RsaVerificationKey2018"],
    };
    if(!(signatureType in supportedSignatures)) {
      throw new NotFoundException("Suite for signature type not found");
    }
    if(!supportedSignatures[signatureType].includes(verificationMethod?.type)) {
      throw new NotFoundException("Suite for verification type not found");
    }
    if(!supportedSignatures[signatureType]) await this.init();
    let keyPair = await this.map[verificationMethod?.type].from(verificationMethod);
    if(withPrivateKey) {
      const privateKeys = await this.vault.readPvtKey(verificationMethod.id.split("#")[0]) || {};
      Object.keys(privateKeys[verificationMethod.id] || {}).forEach(key => {
        keyPair[key] = privateKeys[verificationMethod.id][key];
      });
    }
    return new this.map[signatureType]({key: keyPair});
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
