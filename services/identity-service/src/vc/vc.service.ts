import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { PrismaService } from 'src/prisma.service';
import * as ION from '@decentralized-identity/ion-tools';
import { DidService } from 'src/did/did.service';
import { DIDDocument } from 'did-resolver';
import { VaultService } from 'src/did/vault.service';

@Injectable()
export default class VcService {
  constructor(
    private readonly primsa: PrismaService,
    private readonly didService: DidService,
    private readonly vault: VaultService,
  ) {}

  async sign(signerDID: string, toSign: string) {
    const did = await this.primsa.identity.findUnique({
      where: { id: signerDID },
    });

    if (did) {
      console.log(toSign)
      const signedJWSEd25519 = await ION.signJws({
        payload: toSign,
        privateJwk: await this.vault.readPvtKey(signerDID),
      });
      return {
        publicKey: (JSON.parse(did.didDoc as string) as DIDDocument)
          .verificationMethod[0].publicKeyJwk,
        signed: signedJWSEd25519, // TODO: ADD SUPPORT FOR MORE METHODS (take an input on how to sign while issuing)
      };
    } else {
      throw new NotFoundException('DID not found!');
    }
  }

  async verify(signerDID: string, signedDoc: string): Promise<boolean> {
    const didDocument = await this.didService.resolveDID(signerDID);
    try {
      const verified = await ION.verifyJws({
        jws: signedDoc,
        publicJwk: didDocument.verificationMethod[0].publicKeyJwk,
      });
      Logger.log('verified: ', verified);
      return true;
    } catch (e) {
      return false;
    }
  }
}
