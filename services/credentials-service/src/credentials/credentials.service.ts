import { HttpService } from '@nestjs/axios';
import {
  Injectable,
  InternalServerErrorException,
  NotFoundException,
  StreamableFile,
} from '@nestjs/common';
import { AxiosResponse } from '@nestjs/terminus/dist/health-indicator/http/axios.interfaces';
import { VCStatus, VCV2 } from '@prisma/client';
import { verify } from 'crypto';
import {
  JwtCredentialPayload,
  CredentialPayload,
  transformCredentialInput,
  Verifiable,
  W3CCredential,
} from 'did-jwt-vc';
import { DIDDocument } from 'did-resolver';
import { filter, lastValueFrom, map } from 'rxjs';
import { PrismaService } from '../prisma.service';
import { DeriveCredentialDTO } from './dto/derive-credential.dto';
import { GetCredentialsBySubjectOrIssuer } from './dto/getCredentialsBySubjectOrIssuer.dto';
import { IssueCredentialDTO } from './dto/issue-credential.dto';
import { RenderTemplateDTO } from './dto/renderTemplate.dto';
import { UpdateStatusDTO } from './dto/update-status.dto';
import { VerifyCredentialDTO } from './dto/verify-credential.dto';
import { RENDER_OUTPUT } from './enums/renderOutput.enum';
import { compile, template } from 'handlebars';
import { join } from 'path';
import * as wkhtmltopdf from 'wkhtmltopdf';
import { existsSync, readFileSync, unlinkSync } from 'fs';
import { Proof } from 'src/app.interface';
import { VerifyCredentialResponse } from './dto/verify-response.dto';
import { v4 as uuid } from 'uuid';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const QRCode = require('qrcode');
// eslint-disable-next-line @typescript-eslint/no-var-requires
const ION = require('@decentralized-identity/ion-tools');
@Injectable()
export class CredentialsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly httpService: HttpService,
  ) {}

  async getCredentials(tags: string[]) {
    // console.log('tagsArray', tags);
    const credentials = await this.prisma.vCV2.findMany({
      where: {
        tags: {
          hasSome: [...tags],
        },
      },
    });
    return credentials;
  }

  async getCredentialById(id: string) {
    const credential = await this.prisma.vCV2.findUnique({
      where: { id: id },
      select: {
        signed: true,
      },
    });

    if (!credential)
      throw new NotFoundException('Credential for the given id not found');

    const res = credential.signed;
    delete res['options'];
    delete res['proof'];
    res['id'] = id;
    return res;
  }

  async verifyCredential(credId: string) {
    let credToVerify: any = null;
    credToVerify = await this.prisma.vCV2.findUnique({
      where: {
        id: credId,
      },
    });

    // invalid request in case credential is not found
    if (!credToVerify) {
      throw new NotFoundException({ errors: ['Credential not found'] });
      // return {
      //   errors: ['Credential not found'],
      // };
    }
    try {
      // getting the cred from db

      // no need to verify in case the credential is revoked ? or do I resolve the JWKS anyway
      /*if (credToVerify.status === VCStatus.REVOKED)
        return {
          status: 'revoked',
          checks: [{ revoked: 'OK' }],
        };

      console.log('expiration date: ', credToVerify.expirationDate);
      console.log(
        'credToVerify.expirationDate < new Date(): ',
        credToVerify.expirationDate < new Date(),
      );
      if (new Date(credToVerify.expirationDate).getTime() < Date.now())
        return {
          status: 'expired',
          checks: [{ revoked: 'OK', expired: 'OK' }],
        };*/

      const status = credToVerify.status;
      credToVerify = credToVerify.signed;
      delete credToVerify['options'];

      console.log(
        'process.env.IDENTIY_BASE_URL: ',
        process.env.IDENTITY_BASE_URL,
      );
      const verificationMethod = credToVerify.issuer;
      const verificationURL = `${process.env.IDENTITY_BASE_URL}/did/resolve/${verificationMethod}`;
      console.log('verificationURL: ', verificationURL);
      const dIDResponse: AxiosResponse = await this.httpService.axiosRef.get(
        verificationURL,
      );

      const did: DIDDocument = dIDResponse.data as DIDDocument;
      // console.log('did in verify: ', verify);
      // console.log('credToVerify:', credToVerify);

      // VERIFYING THE JWS
      const verified = await ION.verifyJws({
        jws: credToVerify?.proof?.proofValue,
        publicJwk: did.verificationMethod[0].publicKeyJwk,
      });
      // console.debug(verified);
      // console.log('credToVerify: ', credToVerify);
      return {
        status: status,
        checks: [
          {
            active: 'OK', // not sure what this means
            revoked: status === VCStatus.REVOKED ? 'NOK' : 'OK', // NOK represents revoked
            expired:
              new Date(credToVerify.expirationDate).getTime() < Date.now()
                ? 'NOK'
                : 'OK', // NOK represents expired
            proof: 'OK',
          },
        ],
      };
    } catch (e) {
      console.error(e);
      return {
        errors: [e],
      };
    }
  }

  async signVC(credentialPlayload: JwtCredentialPayload, did: string) {
    // console.log('credentialPlayload: ', credentialPlayload);
    // console.log('did: ', did);
    // did = 'did:ulp:5d7682f4-3cca-40fb-9fa2-1f6ebef4803b';
    const signedVCResponse: AxiosResponse =
      await this.httpService.axiosRef.post(
        `${process.env.IDENTITY_BASE_URL}/utils/sign`,
        {
          DID: did,
          payload: JSON.stringify(credentialPlayload),
        },
      );
    return signedVCResponse.data.signed as string;
  }

  async issueCredential(issueRequest: IssueCredentialDTO) {
    try {
      const credInReq = issueRequest.credential;
      // console.log('credInReq: ', credInReq);
      /*
      //Code block for unsigned credential

      return await this.prisma.vCV2.create({ //use update incase the above codeblock is uncommented 
        data: {
          type: credInReq.type,
          issuer: credInReq.issuer as string,
          issuanceDate: credInReq.issuanceDate,
          expirationDate: credInReq.expirationDate,
          subject: JSON.stringify(credInReq.credentialSubject),
          //proof: credInReq.proof as any,
          credential_schema: JSON.stringify(issueRequest.credentialSchema), //because they can't refer to the schema db from here through an ID
          unsigned: credInReq as object,
        },

      */

      // TODO: Verify the credential with the credential schema using ajv

      credInReq['proof'] = {
        proofValue: await this.signVC(
          transformCredentialInput(credInReq as CredentialPayload),
          credInReq.issuer as string,
        ),
        type: 'Ed25519Signature2020',
        created: new Date().toISOString(),
        verificationMethod: credInReq.issuer,
        proofPurpose: 'assertionMethod',
      };
      console.timeEnd('Sign');
      const id: AxiosResponse = await this.httpService.axiosRef.post(
        `${process.env.IDENTITY_BASE_URL}/did/generate`,
        {
          content: [
            {
              alsoKnownAs: ['did.chinmoy12c@gmail.com.chinmoytest'],
              services: [
                {
                  id: 'IdentityHub',
                  type: 'IdentityHub',
                  serviceEndpoint: {
                    '@context': 'schema.identity.foundation/hub',
                    '@type': 'UserServiceEndpoint',
                    instance: ['did:test:hub.id'],
                  },
                },
              ],
            },
          ],
        },
      );

      credInReq.id = id.data[0]?.id;

      // TODO: add created by and updated by
      const newCred = await this.prisma.vCV2.create({
        //use update incase the above codeblock is uncommented
        data: {
          id: credInReq.id,
          // seqid: seqID.for_next_credential,
          type: credInReq.type,
          issuer: credInReq.issuer as string,
          issuanceDate: credInReq.issuanceDate,
          expirationDate: credInReq.expirationDate,
          subject: credInReq.credentialSubject as any,
          subjectId: (credInReq.credentialSubject as any).id,
          proof: credInReq.proof as any,
          credential_schema: issueRequest.credentialSchemaId, //because they can't refer to the schema db from here through an ID
          signed: credInReq as object,
          tags: issueRequest.tags,
        },
      });

      const res = newCred.signed;
      delete res['options'];
      return {
        credential: res,
        credentialSchemaId: newCred.credential_schema,
        createdAt: newCred.created_at,
        updatedAt: newCred.updated_at,
        createdBy: '',
        updatedBy: '',
        tags: newCred.tags, // TODO: add support for tags
      };
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }

  async deleteCredential(id: string) {
    try {
      const credential = await this.prisma.vCV2.update({
        where: { id: id },
        data: {
          status: 'REVOKED',
        },
      });
      return credential;
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }

  async getCredentialsBySubjectOrIssuer(
    getCreds: GetCredentialsBySubjectOrIssuer,
  ) {
    try {
      const filteringSubject = getCreds.subject;
      const credentials = await this.prisma.vCV2.findMany({
        where: {
          issuer: getCreds.issuer?.id,
          AND: filteringSubject
            ? Object.keys(filteringSubject).map((key) => ({
              subject: {
                path: [key.toString()],
                equals: filteringSubject[key],
              },
            }))
            : [],
        },
        select: {
          id: true,
          signed: true,
        },
      });

      if (credentials.length == 0)
        throw new NotFoundException(
          'No credentials found for the given subject or issuer',
        );

      return credentials.map((cred) => {
        const signed: { [k: string]: any } = cred.signed as any;
        delete signed['id'];
        delete signed['options'];
        delete signed['proof'];
        return { id: cred.id, ...signed };
      });
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }

  async renderCredential(renderingRequest: RenderTemplateDTO) {
    const output = renderingRequest.output;
    const rendering_template = renderingRequest.template;
    const credential = renderingRequest.credential;
    const subject = credential.credentialSubject as any;
    subject.qr = await this.renderAsQR(credential);
    console.log(subject);
    const template = compile(rendering_template);
    const data = template(subject);

    delete subject.id;
    switch (output) {
      case RENDER_OUTPUT.QR:
        const QRData = await this.renderAsQR(credential);
        console.log(QRData);
        return QRData as string;
        break;
      case RENDER_OUTPUT.STRING:
        break;
      case RENDER_OUTPUT.PDF:
        // return new StreamableFile(
        return wkhtmltopdf(data, {
          pageSize: 'A4',
          disableExternalLinks: true,
          disableInternalLinks: true,
          disableJavascript: true,
          encoding: 'UTF-8',
        });
      // );

      case RENDER_OUTPUT.QR_LINK:
        return data;
        break;
      case RENDER_OUTPUT.HTML:
        return data;
        break;
      case RENDER_OUTPUT.STRING:
        break;
      case RENDER_OUTPUT.JSON:
        break;
    }
  }

  // UTILITY FUNCTIONS
  async renderAsQR(cred: VCV2): Promise<any> {
    // const credential = await this.prisma.vCV2.findUnique({
    //   where: { id: credentialId },
    // });

    try {
      // const QRData = await QRCode.toDataURL(
      //   (credential.signed as Verifiable<W3CCredential>).proof.proofValue,
      // );
      const verificationURL = `http://64.227.185.154:3002/credentials/${cred.id}/verify`;
      const QRData = await QRCode.toDataURL(verificationURL);
      return QRData;
    } catch (err) {
      console.error(err);
      return err;
    }
  }

  async getSchemaByCredId(credId: string) {
    try {
      const schema = await this.prisma.vCV2.findUnique({
        where: {
          id: credId,
        },
        select: {
          credential_schema: true,
        },
      });
      return schema;
    } catch (e) {
      console.log(e);
      throw e;
    }
  }
}
