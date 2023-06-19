import { HttpService } from '@nestjs/axios';
import {
  BadRequestException,
  Injectable,
  InternalServerErrorException,
  NotFoundException,
} from '@nestjs/common';
import { AxiosResponse } from '@nestjs/terminus/dist/health-indicator/http/axios.interfaces';
import { VCStatus } from '@prisma/client';
import {
  JwtCredentialPayload,
  CredentialPayload,
  transformCredentialInput,
  Verifiable,
  W3CCredential,
} from 'did-jwt-vc';
import { DIDDocument } from 'did-resolver';
import { PrismaService } from '../prisma.service';
import { GetCredentialsBySubjectOrIssuer } from './dto/getCredentialsBySubjectOrIssuer.dto';
import { IssueCredentialDTO } from './dto/issue-credential.dto';
import { RenderTemplateDTO } from './dto/renderTemplate.dto';
import { RENDER_OUTPUT } from './enums/renderOutput.enum';
import { compile } from 'handlebars';
import * as wkhtmltopdf from 'wkhtmltopdf';
import { IssuerType, Proof } from 'did-jwt-vc/lib/types';
import { JwtCredentialSubject } from 'src/app.interface';
import { getCredentialSchema, verifyCredentialSubject } from './schema.utils';

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

  async getCredentials(tags: ReadonlyArray<string>) {
    const credentials = await this.prisma.verifiableCredentials.findMany({
      where: {
        tags: {
          hasSome: [...tags],
        },
      },
    });
    return credentials;
  }

  async getCredentialById(id: string) {
    const credential = await this.prisma.verifiableCredentials.findUnique({
      where: { id: id },
      select: {
        signed: true,
      },
    });

    if (!credential)
      throw new NotFoundException('Credential for the given id not found');

    // formatting the response as per the spec
    const res = credential.signed;
    delete res['options'];
    res['id'] = id;
    return res;
  }

  async verifyCredential(credId: string) {
    // getting the credential from the db
    const { signed: credToVerify, status } =
      (await this.prisma.verifiableCredentials.findUnique({
        where: {
          id: credId,
        },
        select: {
          signed: true,
          status: true,
        },
      })) as { signed: Verifiable<W3CCredential>; status: VCStatus };

    // invalid request in case credential is not found
    if (!credToVerify) {
      throw new NotFoundException({ errors: ['Credential not found'] });
    }
    try {
      // calling identity service to verify the credential
      const verificationMethod = credToVerify.issuer;
      const verificationURL = `${process.env.IDENTITY_BASE_URL}/did/resolve/${verificationMethod}`;
      const dIDResponse: AxiosResponse = await this.httpService.axiosRef.get(
        verificationURL,
      );

      const did: DIDDocument = dIDResponse.data as DIDDocument;

      // VERIFYING THE JWS
      await ION.verifyJws({
        jws: credToVerify?.proof?.proofValue,
        publicJwk: did.verificationMethod[0].publicKeyJwk,
      });

      return {
        status: status,
        checks: [
          {
            active: 'OK',
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

  async signVC(credentialPlayload: JwtCredentialPayload, did: IssuerType) {
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

      // TODO: Verify the credential with the credential schema using ajv
      // get the credential schema
      const schema = await getCredentialSchema(
        issueRequest.credentialSchemaId,
        this.httpService,
      );
      const { valid, errors } = verifyCredentialSubject(credInReq, schema);
      if (!valid) throw new BadRequestException(errors);

      // generate the DID for credential
      const id: AxiosResponse = await this.httpService.axiosRef.post(
        `${process.env.IDENTITY_BASE_URL}/did/generate`,
        {
          content: [
            {
              alsoKnownAs: ['verifiable credential'],
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

      // sign the credential
      credInReq['proof'] = {
        proofValue: await this.signVC(
          transformCredentialInput(credInReq as CredentialPayload),
          credInReq.issuer,
        ),
        type: 'Ed25519Signature2020',
        created: new Date().toISOString(),
        verificationMethod: credInReq.issuer,
        proofPurpose: 'assertionMethod',
      };

      // TODO: add created by and updated by
      const newCred = await this.prisma.verifiableCredentials.create({
        data: {
          id: credInReq.id,
          type: credInReq.type,
          issuer: credInReq.issuer as IssuerType as string,
          issuanceDate: credInReq.issuanceDate,
          expirationDate: credInReq.expirationDate,
          subject: credInReq.credentialSubject as JwtCredentialSubject,
          subjectId: (credInReq.credentialSubject as JwtCredentialSubject).id,
          proof: credInReq.proof as Proof,
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
        tags: newCred.tags,
      };
    } catch (err) {
      console.log('err: ', err);
      throw new InternalServerErrorException(err);
    }
  }

  async deleteCredential(id: string) {
    try {
      const credential = await this.prisma.verifiableCredentials.update({
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

      const credentials = await this.prisma.verifiableCredentials.findMany({
        where: {
          issuer: getCreds.issuer?.id,
          AND: filteringSubject
            ? Object.keys(filteringSubject).map((key: string) => ({
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
          'No credentials found for the given filters',
        );

      return credentials.map((cred) => {
        const signed: W3CCredential = cred.signed as W3CCredential;
        // formatting the output as per the spec
        delete signed['id'];
        delete signed['options'];
        return { id: cred.id, ...signed };
      });
    } catch (err) {
      throw new InternalServerErrorException(err);
    }
  }

  async renderCredential(renderingRequest: RenderTemplateDTO) {
    const output = renderingRequest.output;
    const rendering_template = renderingRequest.template;
    const credential = renderingRequest.credential as W3CCredential;
    const subject = credential.credentialSubject as any;
    subject.qr = await this.renderAsQR(credential);
    const template = compile(rendering_template);
    const data = template(subject);

    delete subject.id;
    switch (output) {
      case RENDER_OUTPUT.QR:
        const QRData = await this.renderAsQR(credential);
        return QRData as string;
        break;
      case RENDER_OUTPUT.STRING:
        break;
      case RENDER_OUTPUT.PDF:
        return wkhtmltopdf(data, {
          pageSize: 'A4',
          disableExternalLinks: true,
          disableInternalLinks: true,
          disableJavascript: true,
          encoding: 'UTF-8',
        });

      case RENDER_OUTPUT.QR_LINK:
        return data;
      case RENDER_OUTPUT.HTML:
        return data;
      case RENDER_OUTPUT.STRING:
        break;
      case RENDER_OUTPUT.JSON:
        break;
    }
  }

  // UTILITY FUNCTIONS
  async renderAsQR(cred: W3CCredential) {
    try {
      const verificationURL = `${process.env.CREDENTIAL_SERVICE_BASE_URL}/credentials/${cred.id}/verify`;
      const QRData = await QRCode.toDataURL(verificationURL);
      return QRData;
    } catch (err) {
      console.error(err);
      return err;
    }
  }

  async getSchemaByCredId(credId: string) {
    try {
      const schema = await this.prisma.verifiableCredentials.findUnique({
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
