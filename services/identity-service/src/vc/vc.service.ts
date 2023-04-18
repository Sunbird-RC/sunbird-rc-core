import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from 'src/prisma.service';
import * as ION from '@decentralized-identity/ion-tools';
import { HttpService } from '@nestjs/axios';
import { DidService } from 'src/did/did.service';
import { DIDDocument } from 'did-resolver';
import { VaultService } from 'src/did/vault.service';

@Injectable()
export default class VcService {
  constructor(
    private readonly primsa: PrismaService,
    private readonly httpService: HttpService,
    private readonly didService: DidService,
    private readonly vault: VaultService,
  ) {}

  async sign(signerDID: string, toSign: string) {
    console.log('signerDID: ', signerDID);
    console.time("Database");
    const did = await this.primsa.identity.findUnique({
      where: { id: signerDID },
    });
    console.timeEnd("Database");

    if (did) {
      console.log('yes');
      console.time("JWS Sign")
      console.log(toSign)
      const signedJWSEd25519 = await ION.signJws({
        payload: toSign,
        privateJwk: await this.vault.readPvtKey(signerDID),
      });
      console.timeEnd("JWS Sign")
      return {
        publicKey: (JSON.parse(did.didDoc as string) as DIDDocument)
          .verificationMethod[0].publicKeyJwk,
        signed: signedJWSEd25519, // TODO: ADD SUPPORT FOR MORE METHODS (take an input on how to sign while issuing)
      };
    } else {
      throw new NotFoundException('DID not found!');
    }
  }
}
