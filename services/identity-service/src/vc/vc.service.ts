import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../utils/prisma.service';
import { DidService } from '../did/did.service';
import { DIDDocument } from 'did-resolver';
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
import { sign, verify } from '@decentralized-identity/ion-tools';
@Injectable()
export default class VcService {
  constructor(
    private readonly primsa: PrismaService,
    private readonly didService: DidService,
    private readonly vault: VaultService,
  ) {}

  async sign(signerDID: string, toSign: string) {
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
      const signedJWS = await sign({
        payload: toSign,
        privateJwk: await this.vault.readPvtKey(signerDID),
      });
      const didDoc = (JSON.parse(did.didDoc as string) as DIDDocument);
      return {
        publicKey: didDoc.verificationMethod[0].publicKeyJwk,
        type: DidService.getKeySignType(didDoc.verificationMethod[0].publicKeyJwk?.crv).signType,
        created: new Date().toISOString(),
        verificationMethod: didDoc?.verificationMethod[0]?.id,
        proofPurpose: 'assertionMethod',
        jws: signedJWS,
      };
    } catch (err) {
      Logger.error('Error signign the document:', JSON.stringify(err, null, 4));
      throw new InternalServerErrorException(`Error signign the document`);
    }
  }

  async verify(signerDID: string, signedDoc: string): Promise<boolean> {
    let didDocument: DIDDocument;
    try {
      didDocument = await this.didService.resolveDID(signerDID);
    } catch (err) {
      Logger.error(`Error resolving signer did: `, err);
      throw new InternalServerErrorException(`Error resolving signer did`);
    }

    try {
      const verified = await verify({
        jws: signedDoc,
        publicJwk: didDocument.verificationMethod[0].publicKeyJwk,
      });
      if (verified) return true;
      return false;
    } catch (e) {
      Logger.error(e);
      return false;
    }
  }
}
