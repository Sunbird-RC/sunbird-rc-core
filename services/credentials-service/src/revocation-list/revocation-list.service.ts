import { HttpService } from '@nestjs/axios';
import { Injectable, InternalServerErrorException, Logger } from '@nestjs/common';
import { CredentialPayload, transformCredentialInput } from 'did-jwt-vc';
import { DIDDocument } from 'did-resolver';
import { IdentityUtilsService } from '../credentials/utils/identity.utils.service';
// import { PrismaService } from 'src/prisma.service';
import { PrismaClient } from '@prisma/client';
import { IssuerType, Proof } from 'did-jwt-vc/lib/types';
import { JwtCredentialSubject } from 'src/app.interface';
import { RevocationLists, VerifiableCredentials } from '@prisma/client';
import { RevocationListImpl } from './revocation-list.impl';
import { RevocationList } from './revocation-list.helper';

@Injectable()
export class RevocationListService {
  constructor(
    private readonly prismaService: PrismaClient,
    private readonly rl: RevocationListImpl,
    private readonly identityService: IdentityUtilsService) {}

  private async signRevocationListCredential(revocationListCredential, issuer) {
    try {
      revocationListCredential['proof'] = {
        proofValue: await this.identityService.signVC(
          transformCredentialInput(revocationListCredential as CredentialPayload),
          issuer,
        ),
        type: 'Ed25519Signature2020',
        created: new Date().toISOString(),
        verificationMethod: issuer,
      };
    } catch (err) {
      Logger.error('Error signing revocation list', err);
      throw new InternalServerErrorException(
        'Error signing revocation list',
      );
    }

    return revocationListCredential.proof;
  }

  private generateRevocationListCredentialSkeleton(id: string, issuer: string) {
    return {
      "@context": [
        "https://www.w3.org/2018/credentials/v1",
        "https://w3id.org/vc-revocation-list-2020/v1"
      ],
      id,
      type: ["VerifiableCredential", "RevocationList2020Credential"],
      issuer,
      issuanceDate: new Date().toISOString(),
      credentialSubject: {
        id,
        type: "RevocationList2020",
      },
      proof: {}
    };
  }
  async createNewRevocationList(issuer: string) {
    // generate did for the revocation list
    let credDID: ReadonlyArray<DIDDocument>;
    try {
      credDID = await this.identityService.generateDID(['verifiable credential']);
    } catch (err) {
      Logger.error('Error generating DID for revocation list', err);
      throw new InternalServerErrorException(
        'Error generating DID for revocation list',
      )
    }

    const revocationList = await this.rl.createList({ length: 100000 });
    const encodedList = await revocationList.encode();

    const revocationListCredential = this.generateRevocationListCredentialSkeleton(credDID[0]?.id, issuer);
    revocationListCredential.credentialSubject['encodedList'] = encodedList;
    // sign the revocation list
    revocationListCredential.proof = await this.signRevocationListCredential(revocationListCredential, issuer);


    // save this in the db
    try {
      const revocationListsOfIssuer = await this.prismaService.revocationLists.findUnique({
        where: {
          issuer
        }
      });
      if (!revocationListsOfIssuer) {
        await this.prismaService.$transaction([
          // save the revocation list as a verifiable credential
          this.prismaService.verifiableCredentials.create({
            data: {
              id: revocationListCredential.id,
              type: revocationListCredential.type,
              issuer: revocationListCredential.issuer as IssuerType as string,
              issuanceDate: revocationListCredential.issuanceDate,
              expirationDate: '',
              subject: revocationListCredential.credentialSubject as JwtCredentialSubject,
              subjectId: (revocationListCredential.credentialSubject as JwtCredentialSubject).id,
              proof: revocationListCredential.proof as Proof,
              credential_schema: '', // HOST A JSONLD for this in the github repo and link to that
              signed: revocationListCredential as object,
              tags: ['RevocationList2020Credential', 'RevocationList2020'],
            }
          }),
          // update the revocation list data

          this.prismaService.revocationLists.create({
            data: {
              issuer,
              latestRevocationListId: revocationListCredential.id,
              lastCredentialIdx: 0,
              allRevocationLists: [revocationListCredential.id]
            }
          })
        ])
      } else {
        await this.prismaService.$transaction([
          this.prismaService.verifiableCredentials.create({
            data: {
              id: revocationListCredential.id,
              type: revocationListCredential.type,
              issuer: revocationListCredential.issuer as IssuerType as string,
              issuanceDate: revocationListCredential.issuanceDate,
              expirationDate: '',
              subject: revocationListCredential.credentialSubject as JwtCredentialSubject,
              subjectId: (revocationListCredential.credentialSubject as JwtCredentialSubject).id,
              proof: revocationListCredential.proof as Proof,
              credential_schema: '', // HOST A JSONLD for this in the github repo and link to that
              signed: revocationListCredential as object,
              tags: ['RevocationList2020Credential', 'RevocationList2020'],
            }
          }),
          // update the revocation list data
          this.prismaService.revocationLists.update({
            where: {
              issuer
            },
            data: {
              latestRevocationListId: revocationListCredential.id,
              lastCredentialIdx: 0,
              allRevocationLists: [revocationListCredential.id, ...revocationListsOfIssuer.allRevocationLists]
            }
          })
        ])
      }
    } catch (err) {
      Logger.error('Error saving the revocation list credential into db: ', err);
      throw new InternalServerErrorException(
        'Error saving the revocation list credential into db',
      );
    }

    return revocationListCredential;
  }

  async updateRevocationList(issuer: string, idx: number) {
    // fetch the revocation list from the db
    let revocationList: VerifiableCredentials;
    let revocationListInfo: RevocationLists;
    try {
      revocationListInfo = await this.prismaService.revocationLists.findUnique({
        where: {
          issuer
        }
      });

      revocationList = await this.prismaService.verifiableCredentials.findUnique({
        where: {
          id: revocationListInfo.latestRevocationListId
        }
      });
    } catch (err) {
      Logger.error('Error fetching revocation list from db', err);
      throw new InternalServerErrorException(
        'Error fetching revocation list from db',
      );
    }

    let revocationListCredential;
    try {
      const encodedList = (revocationList.subject as any).encodedList;
      const decodedList: RevocationList = await this.rl.decodeList({ encodedList });
      decodedList.setRevoked(idx, true);
      const updatedEncodedList = await decodedList.encode();
      // update the RevocationListCredential by resigning it
      revocationListCredential = this.generateRevocationListCredentialSkeleton(revocationList.id, issuer);
      revocationListCredential.credentialSubject['encodedList'] = updatedEncodedList;
    } catch (err) {
      Logger.error('Error updating the revocation list bits: ', err);
      throw new InternalServerErrorException(
        'Error updating the revocation list bits',
      );
    }
    // sign this again
    try {
      revocationListCredential.proof = await this.signRevocationListCredential(revocationListCredential, issuer);
    } catch (err) {
      Logger.error('Error signing the revocation list credential', err);
      throw new InternalServerErrorException(
        'Error signing the revocation list credential',
      );
    }

    // update the db
    let revocationListCredentialId = revocationListInfo.latestRevocationListId;
    try {
      // update the proof of revocation list credential
      await this.prismaService.verifiableCredentials.update({
        where: {
          id: revocationList.id
        },
        data: {
          proof: revocationListCredential.proof as Proof,
        }
      });
      if ((revocationListInfo.lastCredentialIdx) < 100000) {
        // update the counter of index in the revocation list
        this.prismaService.revocationLists.update({
          where: {
            issuer
          },
          data: {
            lastCredentialIdx: idx + 1,
          }
        })
      } else {
        // create a new revocation list
        const newRevocationList = await this.createNewRevocationList(issuer);
        revocationListCredentialId = newRevocationList.id;
      }
    } catch (err) {
      Logger.error('Error updating the revocation list credential into db: ', err);
      throw new InternalServerErrorException(
        'Error updating the revocation list credential into db',
      );
    }

    return revocationListCredentialId;
  }

  async getDecodedRevocationString(revocationCredentialId: string) {
    // fetch the credential from db
    try {
      const revocationListCredential = await this.prismaService.verifiableCredentials.findUnique({
        where: {
          id: revocationCredentialId
        }
      });
      const encodedList = (revocationListCredential.subject as any).encodedList;
      const decodedList = await this.rl.decodeList({ encodedList });
      return decodedList;
    } catch (err) {
      Logger.error('Error fetching the RevocationListCredential from db', err);
      throw new InternalServerErrorException(
        'Error fetching the RevocationListCredential from db',
      );
    }
  }
} 
