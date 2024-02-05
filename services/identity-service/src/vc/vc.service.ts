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
      const didDoc = (JSON.parse(did.didDoc as string) as DIDDocument);
      const verificationMethod = didDoc.verificationMethod[0];
      const signedJWS = await sign({
        payload: toSign,
        privateJwk: await this.vault.readPvtKey(verificationMethod?.id),
      });
      return {
        publicKey: verificationMethod?.publicKeyJwk,
        type: DidService.getKeySignType(verificationMethod?.publicKeyJwk?.crv).signType,
        created: new Date().toISOString(),
        verificationMethod: verificationMethod?.id,
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
      const publicJwk = didDocument?.verificationMethod[0]?.publicKeyJwk;
      const verified = await verify({ jws: signedDoc, publicJwk });
      return verified;
    } catch (e) {
      Logger.error(e);
      return false;
    }
  }
}
