import { Injectable, InternalServerErrorException, Logger } from '@nestjs/common';
import { PrismaClient } from '@prisma/client';
import { IdentityUtilsService } from './identity.utils.service';
import { RevocationListImpl } from '../../revocation-list/revocation-list.impl';
import { CredentialPayload, DIDDocument, IssuerType, Proof } from 'vc.types';
import { JwtCredentialSubject } from 'src/app.interface';

const LIST_LENGTH = 100000;

// StatusList / RevocationList2020 handling, wired into issue / verify / revoke.
// This finally makes the previously-dead bitstring code active:
//  - allocateIndex(): reserve a bit for a newly issued VC (any format)
//  - buildCredentialStatus(): the `credentialStatus` block embedded in the VC
//  - setRevoked(): flip the bit + re-sign the status-list credential
//  - isRevoked(): consulted inside verifyCredential
@Injectable()
export class StatusListService {
  private logger = new Logger(StatusListService.name);

  constructor(
    private readonly prisma: PrismaClient,
    private readonly rl: RevocationListImpl,
    private readonly identityUtilsService: IdentityUtilsService
  ) {}

  // Reserve the next free index in the issuer's status list, creating the list
  // (and its signed StatusList credential) on first use.
  async allocateIndex(
    issuer: string
  ): Promise<{ statusListCredential: string; index: number }> {
    let info = await this.prisma.revocationLists.findUnique({ where: { issuer } });
    if (!info) {
      const created = await this.createNewList(issuer);
      info = created;
    }
    const index = info.lastCredentialIdx;
    if (index >= LIST_LENGTH - 1) {
      const created = await this.createNewList(issuer);
      return { statusListCredential: created.latestRevocationListId, index: 0 };
    }
    await this.prisma.revocationLists.update({
      where: { issuer },
      data: { lastCredentialIdx: index + 1 },
    });
    return { statusListCredential: info.latestRevocationListId, index };
  }

  buildCredentialStatus(statusListCredential: string, index: number) {
    return {
      id: `${statusListCredential}#${index}`,
      type: 'RevocationList2020Status',
      revocationListIndex: `${index}`,
      revocationListCredential: statusListCredential,
    };
  }

  async isRevoked(statusListCredential: string, index: number): Promise<boolean> {
    try {
      const listVc = await this.prisma.verifiableCredentials.findUnique({
        where: { id: statusListCredential },
      });
      if (!listVc) return false;
      const encodedList = (listVc.subject as any)?.encodedList;
      if (!encodedList) return false;
      const decoded = await this.rl.decodeList({ encodedList });
      return !!decoded.isRevoked(index);
    } catch (err) {
      this.logger.error('Error checking revocation status', err);
      return false;
    }
  }

  async setRevoked(statusListCredential: string, index: number, issuer: string) {
    try {
      const listVc = await this.prisma.verifiableCredentials.findUnique({
        where: { id: statusListCredential },
      });
      if (!listVc) throw new InternalServerErrorException('Status list credential not found');
      const encodedList = (listVc.subject as any).encodedList;
      const decoded = await this.rl.decodeList({ encodedList });
      decoded.setRevoked(index, true);
      const updatedEncoded = await decoded.encode();

      const skeleton = this.listSkeleton(statusListCredential, issuer, updatedEncoded);
      const proof = await this.signList(skeleton, issuer);
      skeleton.proof = proof;

      await this.prisma.verifiableCredentials.update({
        where: { id: statusListCredential },
        data: {
          subject: skeleton.credentialSubject as JwtCredentialSubject,
          proof: proof as Proof,
          signed: skeleton as object,
        },
      });
    } catch (err) {
      this.logger.error('Error setting revocation bit', err);
      throw new InternalServerErrorException('Error updating status list');
    }
  }

  private async createNewList(issuer: string) {
    let credDID: ReadonlyArray<DIDDocument>;
    try {
      credDID = await this.identityUtilsService.generateDID(['status list']);
    } catch (err) {
      this.logger.error('Error generating DID for status list', err);
      throw new InternalServerErrorException('Error generating DID for status list');
    }
    const id = credDID[0]?.id;
    const list = this.rl.createList({ length: LIST_LENGTH });
    const encodedList = await list.encode();
    const skeleton = this.listSkeleton(id, issuer, encodedList);
    skeleton.proof = await this.signList(skeleton, issuer);

    try {
      const existing = await this.prisma.revocationLists.findUnique({ where: { issuer } });
      await this.prisma.$transaction([
        this.prisma.verifiableCredentials.create({
          data: {
            id: skeleton.id,
            type: skeleton.type,
            issuer: skeleton.issuer as IssuerType as string,
            issuanceDate: skeleton.issuanceDate,
            expirationDate: '',
            subject: skeleton.credentialSubject as JwtCredentialSubject,
            subjectId: (skeleton.credentialSubject as JwtCredentialSubject).id,
            proof: skeleton.proof as Proof,
            credential_schema: '',
            signed: skeleton as object,
            tags: ['RevocationList2020Credential', 'RevocationList2020'],
          },
        }),
        existing
          ? this.prisma.revocationLists.update({
              where: { issuer },
              data: {
                latestRevocationListId: skeleton.id,
                lastCredentialIdx: 0,
                allRevocationLists: [skeleton.id, ...existing.allRevocationLists],
              },
            })
          : this.prisma.revocationLists.create({
              data: {
                issuer,
                latestRevocationListId: skeleton.id,
                lastCredentialIdx: 0,
                allRevocationLists: [skeleton.id],
              },
            }),
      ]);
    } catch (err) {
      this.logger.error('Error persisting status list', err);
      throw new InternalServerErrorException('Error persisting status list');
    }
    return await this.prisma.revocationLists.findUnique({ where: { issuer } });
  }

  private listSkeleton(id: string, issuer: string, encodedList: string): any {
    return {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://w3id.org/vc-revocation-list-2020/v1',
      ],
      id,
      type: ['VerifiableCredential', 'RevocationList2020Credential'],
      issuer,
      issuanceDate: new Date().toISOString(),
      credentialSubject: {
        id,
        type: 'RevocationList2020',
        encodedList,
      },
      proof: {},
    };
  }

  private async signList(skeleton: any, issuer: string) {
    const signed = (await this.identityUtilsService.signVC(
      skeleton as CredentialPayload,
      issuer
    )) as any;
    return signed?.proof;
  }

  async getStatusListCredential(id: string) {
    const listVc = await this.prisma.verifiableCredentials.findUnique({ where: { id } });
    if (!listVc) return null;
    return listVc.signed;
  }
}
