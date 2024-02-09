import {
  BadRequestException,
  Injectable,
  InternalServerErrorException,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { PrismaClient, VCStatus, VerifiableCredentials } from '@prisma/client';
import { GetCredentialsBySubjectOrIssuer } from './dto/getCredentialsBySubjectOrIssuer.dto';
import { IssueCredentialDTO } from './dto/issue-credential.dto';
import { RENDER_OUTPUT } from './enums/renderOutput.enum';
import { JwtCredentialSubject } from 'src/app.interface';
import { SchemaUtilsSerivce } from './utils/schema.utils.service';
import { IdentityUtilsService } from './utils/identity.utils.service';
import { RenderingUtilsService } from './utils/rendering.utils.service';
import * as jsigs from 'jsonld-signatures';
import * as jsonld from 'jsonld';
import { DOCUMENTS } from './documents';
const AssertionProofPurpose = jsigs.purposes.AssertionProofPurpose;
import { 
  W3CCredential, Verifiable, DIDDocument,
  CredentialPayload, IssuerType, Proof, VerificationMethod
 } from 'vc.types';

@Injectable()
export class CredentialsService {
  map = {
    Ed25519VerificationKey2020: null,
    Ed25519Signature2020: null,
    vc: null
  };
  documents: object
  constructor(
    private readonly prisma: PrismaClient,
    private readonly identityUtilsService: IdentityUtilsService,
    private readonly renderingUtilsService: RenderingUtilsService,
    private readonly schemaUtilsService: SchemaUtilsSerivce
  ) {
    this.init();
  }

  async init() {
    const {Ed25519VerificationKey2020} = await import('@digitalbazaar/ed25519-verification-key-2020');
    const {Ed25519Signature2020} = await import('@digitalbazaar/ed25519-signature-2020');
    const vc = await import('@digitalbazaar/vc');
    this.map.Ed25519VerificationKey2020 = Ed25519VerificationKey2020;
    this.map.Ed25519Signature2020 = Ed25519Signature2020;
    this.map.vc = vc;
  }

  private logger = new Logger(CredentialsService.name);

  async getCredentials(
    tags: ReadonlyArray<string>,
    page = 1,
    limit = 20
  ): Promise<ReadonlyArray<W3CCredential>> {
    const credentials = await this.prisma.verifiableCredentials.findMany({
      where: {
        tags: {
          hasSome: [...tags],
        },
      },
      skip: (page - 1) * limit,
      take: limit,
      orderBy: {
        issuanceDate: 'desc',
      },
    });

    if (!credentials) {
      this.logger.error('Error fetching credentials');
      throw new InternalServerErrorException('Error fetching credentials');
    }
    return credentials.map((cred: VerifiableCredentials) => {
      const res = cred.signed;
      delete res['options'];
      res['id'] = cred.id;
      return res as W3CCredential;
    }) as ReadonlyArray<W3CCredential>;
  }

  async getCredentialById(
    id: string,
    templateId: string = null,
    output: string = 'json' // : Promise<W3CCredential>
  ) {
    const credential = await this.prisma.verifiableCredentials.findUnique({
      where: { id: id },
      select: {
        signed: true,
      },
    });
    if (!credential) {
      this.logger.error('Credential not found');
      throw new NotFoundException('Credential for the given id not found');
    }
    // formatting the response as per the spec
    const res = credential.signed;
    delete res['options'];
    res['id'] = id;
    let template = null;

    switch (output.toUpperCase()) {
      case RENDER_OUTPUT.QR:
        const QRData = await this.renderingUtilsService.generateQR(
          res as W3CCredential
        );
        return QRData as string;
      case RENDER_OUTPUT.PDF:
        // fetch the template
        // TODO: Add type here
        template = await this.schemaUtilsService.getTemplateById(templateId);
        return this.renderingUtilsService.renderAsPDF(
          res as W3CCredential,
          template.template
        );
      case RENDER_OUTPUT.HTML:
        template = await this.schemaUtilsService.getTemplateById(templateId);
        return await this.renderingUtilsService.compileHBSTemplate(
          res as W3CCredential,
          template.template
        );
      case RENDER_OUTPUT.STRING:
        return JSON.stringify(res);
      case RENDER_OUTPUT.JSON:
        return res as W3CCredential;
      default:
        this.logger.error('Output type not supported');
        throw new BadRequestException('Output type not supported');
    }
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

    this.logger.debug('Fetched credntial from db to verify');

    // invalid request in case credential is not found
    if (!credToVerify) {
      this.logger.error('Credential not found');
      throw new NotFoundException({ errors: ['Credential not found'] });
    }
    try {
      // calling identity service to verify the issuer DID
      const verificationMethod = credToVerify.issuer;
      const did: DIDDocument = await this.identityUtilsService.resolveDID(
        verificationMethod
      );
      const credVerificationMethod = credToVerify?.proof?.verificationMethod

      // VERIFYING THE JWS
      const vm = did.verificationMethod?.find(d => d.id === credVerificationMethod);
      const suite = await this.getSuite(vm, credToVerify?.proof?.type);
      const results = await this.map.vc.verifyCredential({
        credential: credToVerify,
        purpose: new AssertionProofPurpose(),
        suite: [suite],
        documentLoader: this.getDocumentLoader(did),
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
            proof: !!results?.verified ? 'OK' : 'NOK',
          },
        ],
      };
    } catch (e) {
      this.logger.error('Error in verifying credentials: ', e);
      return {
        errors: [e],
      };
    }
  }

  async getSuite(verificationMethod: VerificationMethod, signatureType: string) {
    if(signatureType !== "Ed25519Signature2020") {
      throw new NotFoundException("Suite for signature type not found");
    }
    const supportedMethods = ["Ed25519VerificationKey2020", "JsonWebKey2020", "Ed25519VerificationKey2018"];
    if(!supportedMethods.includes(verificationMethod?.type)) {
      throw new NotFoundException("Suite for verification method type not found");
    }
    const keyPair = await this.map.Ed25519VerificationKey2020.from(verificationMethod);
    return new this.map.Ed25519Signature2020({key: keyPair});
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

  async issueCredential(issueRequest: IssueCredentialDTO) {
    this.logger.debug(`Received issue credential request`);
    const credInReq = issueRequest.credential;
    // check for issuance date
    if (!credInReq.issuanceDate)
      credInReq.issuanceDate = new Date(Date.now()).toISOString();
    // Verify the credential with the credential schema using ajv
    // get the credential schema
    const schema = await this.schemaUtilsService.getCredentialSchema(
      issueRequest.credentialSchemaId,
      issueRequest.credentialSchemaVersion
    );
    this.logger.debug('fetched schema');
    const { valid, errors } =
      await this.schemaUtilsService.verifyCredentialSubject(
        credInReq,
        schema.schema
      );
    if (!valid) {
      this.logger.error('Invalid credential schema', errors);
      throw new BadRequestException(errors);
    }
    this.logger.debug('validated schema');
    // generate the DID for credential
    const credDID: ReadonlyArray<DIDDocument> =
      await this.identityUtilsService.generateDID(
        [],
        issueRequest.method
      );
    this.logger.debug('generated DID');
    try {
      credInReq.id = credDID[0].id;
    } catch (err) {
      this.logger.error('Invalid response from generate DID', err);
      throw new InternalServerErrorException('Problem creating DID');
    }
    // sign the credential
    let signedCredential: W3CCredential = {};
    try {
      signedCredential = await this.identityUtilsService.signVC(
        credInReq as CredentialPayload,
        credInReq.issuer
      );
    } catch (err) {
      this.logger.error('Error signing the credential');
      throw new InternalServerErrorException('Problem signing the credential');
    }

    this.logger.debug('signed credential');

    // TODO: add created by and updated by
    const newCred = await this.prisma.verifiableCredentials.create({
      data: {
        id: signedCredential.id,
        type: signedCredential.type,
        issuer: signedCredential.issuer as IssuerType as string,
        issuanceDate: signedCredential.issuanceDate,
        expirationDate: signedCredential.expirationDate,
        subject: signedCredential.credentialSubject as JwtCredentialSubject,
        subjectId: (signedCredential.credentialSubject as JwtCredentialSubject).id,
        proof: signedCredential.proof as Proof,
        credential_schema: issueRequest.credentialSchemaId, //because they can't refer to the schema db from here through an ID
        signed: signedCredential as object,
        tags: issueRequest.tags,
      },
    });

    if (!newCred) {
      this.logger.error('Problem saving credential to db');
      throw new InternalServerErrorException('Problem saving credential to db');
    }
    this.logger.debug('saved credential to db');

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
      this.logger.error('Error revoking/soft deleting the credential: ', err);
      throw new InternalServerErrorException('Error deleting the credential.');
    }
  }

  async getCredentialsBySubjectOrIssuer(
    getCreds: GetCredentialsBySubjectOrIssuer,
    page = 1,
    limit = 5
  ) {
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
      skip: (page - 1) * limit,
      take: limit,
      orderBy: {
        issuanceDate: 'desc',
      },
    });

    if (!credentials.length) {
      this.logger.error('Error fetching credentials');
      throw new InternalServerErrorException('Error fetching credentials');
    }

    return credentials.map((cred) => {
      const signed: W3CCredential = cred.signed as W3CCredential;
      // formatting the output as per the spec
      delete signed['id'];
      delete signed['options'];
      return { id: cred.id, ...signed };
    });
  }
}
