import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../utils/prisma.service';
import * as ION from '@decentralized-identity/ion-tools';
import { DidService } from '../did/did.service';
import { DIDDocument } from 'did-resolver';
import { VaultService } from '../utils/vault.service';
import { Identity } from '@prisma/client';
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
      const signedJWS = await ION.signJws({
        payload: toSign,
        privateJwk: await this.vault.readPvtKey(signerDID),
      });
      return {
        publicKey: (JSON.parse(did.didDoc as string) as DIDDocument)
          .verificationMethod[0].publicKeyJwk,
        signed: signedJWS,
      };
    } catch (err) {
      Logger.error('Error signign the document:', err);
      throw new InternalServerErrorException(`Error signign the document`);
    }
  }

  async verify(signerDID: string, signedDoc: string): Promise<boolean> {
    let didDocument: DIDDocument;
    try {
      didDocument = await this.didService.resolveDID(signerDID);
    } catch (err) {
      Logger.error(`Error resolving signed did: `, err);
      throw new InternalServerErrorException(`Error resolving signed did`);
    }

    try {
      const verified = await ION.verifyJws({
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
