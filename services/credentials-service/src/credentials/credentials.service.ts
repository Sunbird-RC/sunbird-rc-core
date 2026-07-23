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
import { CredentialFormatService } from './utils/credential-format.service';
import { StatusListService } from './utils/status-list.service';
import { CredentialFormat } from './dto/issue-credential.dto';
import * as jsigs from 'jsonld-signatures';
import * as jsonld from 'jsonld';
import { DOCUMENTS } from './documents';
import { RSAKeyPair } from 'crypto-ld';
const AssertionProofPurpose = jsigs.purposes.AssertionProofPurpose;
import {
  W3CCredential, Verifiable, DIDDocument,
  Proof, VerificationMethod
 } from 'vc.types';
import { SignResult } from './utils/credential-format.service';
import { RevocationListDTO } from './dto/revocaiton-list.dto';

@Injectable()
export class CredentialsService {
  map = {
    Ed25519VerificationKey2020: null,
    JsonWebKey2020: null,
    Ed25519Signature2020: null,
    Ed25519VerificationKey2018: null,
    Ed25519Signature2018: null,
    RsaVerificationKey2018: null,
    RsaSignature2018: null,
    vc: null
  };
  documents: object
  // Status-list embedding is opt-in so the default ldp_vc path stays
  // byte-for-byte identical to today (regression requirement).
  private statusListEnabled = process.env.STATUS_LIST_ENABLED === 'true';

  constructor(
    private readonly prisma: PrismaClient,
    private readonly identityUtilsService: IdentityUtilsService,
    private readonly renderingUtilsService: RenderingUtilsService,
    private readonly schemaUtilsService: SchemaUtilsSerivce,
    private readonly credentialFormatService: CredentialFormatService,
    private readonly statusListService: StatusListService
  ) {
    this.init();
  }

  async init() {
    const vc = await import('@digitalbazaar/vc');
    const {Ed25519VerificationKey2020} = await import('@digitalbazaar/ed25519-verification-key-2020');
    const {Ed25519Signature2020} = await import('@digitalbazaar/ed25519-signature-2020');
    const {Ed25519VerificationKey2018} = await import('@digitalbazaar/ed25519-verification-key-2018');
    const {Ed25519Signature2018} = await import('@digitalbazaar/ed25519-signature-2018');
    this.map.Ed25519VerificationKey2020 = Ed25519VerificationKey2020;
    this.map.JsonWebKey2020 = Ed25519VerificationKey2020;
    this.map.Ed25519Signature2020 = Ed25519Signature2020;
    this.map.Ed25519VerificationKey2018 = Ed25519VerificationKey2018;
    this.map.Ed25519Signature2018 = Ed25519Signature2018;
    this.map.RsaSignature2018 = jsigs.suites.RsaSignature2018;
    this.map.RsaVerificationKey2018 = RSAKeyPair;
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
    externalTemplate: string = null,
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
        template = externalTemplate || (await this.schemaUtilsService.getTemplateById(templateId))?.template;
        return this.renderingUtilsService.renderAsPDF(
          res as W3CCredential,
          template
        );
      case RENDER_OUTPUT.HTML:
        template = externalTemplate || (await this.schemaUtilsService.getTemplateById(templateId))?.template;
        return await this.renderingUtilsService.compileHBSTemplate(
          res as W3CCredential,
          template
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

  async verifyCredential(
    credToVerify: Verifiable<W3CCredential> | string,
    status?: VCStatus,
    options?: { challenge?: string; domain?: string }
  ) {
    // Enveloped formats (jwt_vc_json / vc+sd-jwt) arrive as compact strings.
    if (typeof credToVerify === 'string') {
      return this.verifyEnvelopedCredential(credToVerify, status, options);
    }
    try {
      // calling identity service to verify the issuer DID
      const issuerId = (credToVerify.issuer?.id || credToVerify.issuer) as string;
      const did: DIDDocument = await this.identityUtilsService.resolveDID(
        issuerId
      );
      const credVerificationMethod = (credToVerify?.proof || {})[Object.keys(credToVerify?.proof || {})
        .find(d => d.indexOf("verificationMethod") > -1)]

      // VERIFYING THE JWS
      const vm = did.verificationMethod?.find(d => (d.id === credVerificationMethod || d.id === credVerificationMethod?.id));
      const suite = await this.getSuite(vm, credToVerify?.proof?.type);
      let results;
      if(credToVerify?.proof?.type === "RsaSignature2018") {
        this.map.vc._checkCredential({
          credential: credToVerify
        })
        results = await jsigs.verify(credToVerify, {
          purpose: new AssertionProofPurpose(),
          suite: [suite],
          documentLoader: this.getDocumentLoader(did),
          addSuiteContext: true,
          compactProof: false
        });
      } else {
        results = await this.map.vc.verifyCredential({
          credential: credToVerify,
          purpose: new AssertionProofPurpose(),
          suite: [suite],
          documentLoader: this.getDocumentLoader(did)
        });
      }
      if(!results?.verified) {
        this.logger.error('Error in verifying credentials: ', results);
      }

      // Replay protection (P0.5): only enforced when the caller supplies
      // challenge/domain — existing callers that omit them are unaffected.
      const replay = this.checkChallengeDomain(credToVerify?.proof, options);

      // StatusList revocation check (P0.6): consult the bitstring list when the
      // credential carries a credentialStatus and status-list mode is enabled.
      const statusListRevoked = await this.checkStatusList(credToVerify);
      const revoked =
        status === VCStatus.REVOKED || statusListRevoked ? 'NOK' : 'OK';

      const proofOk = !!results?.verified && replay.ok;
      return {
        status: status,
        checks: [
          {
            ...((status || statusListRevoked !== null) && { revoked }),
            expired:
              new Date(credToVerify.expirationDate).getTime() < Date.now()
                ? 'NOK'
                : 'OK', // NOK represents expired
            proof: proofOk ? 'OK' : 'NOK',
            ...(replay.checked && { replay: replay.ok ? 'OK' : 'NOK' }),
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

  // Verifies enveloped (jwt_vc_json / vc+sd-jwt) credentials by delegating the
  // signature check to identity-service, then applying the same expiry, status,
  // and replay checks as the JSON-LD path.
  private async verifyEnvelopedCredential(
    compact: string,
    status?: VCStatus,
    options?: { challenge?: string; domain?: string }
  ) {
    try {
      const isSdJwt = compact.includes('~');
      // A compact JWS always has exactly 2 '.' separators; base64url (our
      // mdoc envelope) never contains '.' at all — an unambiguous, cheap
      // shape check without needing the caller to pass the format down.
      const isJwt = !isSdJwt && compact.includes('.');
      const isMdoc = !isSdJwt && !isJwt;
      let verified: boolean;
      let claims: any;
      let mdocDocType: string | undefined;
      if (isSdJwt) {
        const res = await this.identityUtilsService.verifySdJwt(compact, undefined, {
          nonce: options?.challenge,
          audience: options?.domain,
        });
        verified = res.verified;
        claims = res.claims;
      } else if (isMdoc) {
        const res = await this.identityUtilsService.verifyMdoc(compact);
        verified = res.verified;
        claims = res.claims;
        mdocDocType = res.docType;
      } else {
        const res = await this.identityUtilsService.verifyJwt(compact);
        verified = res.verified;
        claims = res.payload?.vc || res.payload;
      }

      // mdoc has no exp/expirationDate concept at this level (validity is
      // its own validityInfo, already checked inside verifyMdoc's digest
      // pass) — treat as non-expiring here rather than misreading namespace
      // claims as JWT-shaped expiry fields.
      const expEpoch = isMdoc
        ? undefined
        : claims?.exp || (claims?.expirationDate ? new Date(claims.expirationDate).getTime() / 1000 : undefined);
      const expired = expEpoch && expEpoch * 1000 < Date.now() ? 'NOK' : 'OK';

      const credentialStatus = claims?.credentialStatus || claims?.vc?.credentialStatus;
      const statusListRevoked = await this.checkStatusListEntry(credentialStatus);
      const revoked = status === VCStatus.REVOKED || statusListRevoked ? 'NOK' : 'OK';

      return {
        status,
        checks: [
          {
            ...((status || statusListRevoked !== null) && { revoked }),
            expired,
            proof: verified ? 'OK' : 'NOK',
          },
        ],
        ...(mdocDocType ? { docType: mdocDocType } : {}),
      };
    } catch (e) {
      this.logger.error('Error verifying enveloped credential: ', e);
      return { errors: [e] };
    }
  }

  // Compares LD-proof challenge/domain against caller-supplied options.
  // Returns { checked:false } when the caller passes nothing (existing behaviour).
  private checkChallengeDomain(
    proof: any,
    options?: { challenge?: string; domain?: string }
  ): { checked: boolean; ok: boolean } {
    if (!options || (!options.challenge && !options.domain)) {
      return { checked: false, ok: true };
    }
    let ok = true;
    if (options.challenge && proof?.challenge !== options.challenge) ok = false;
    if (options.domain && proof?.domain !== options.domain) ok = false;
    return { checked: true, ok };
  }

  // Returns true/false when a status can be determined, null when there is no
  // credentialStatus to check (so the caller can omit the 'revoked' field).
  private async checkStatusList(cred: any): Promise<boolean | null> {
    return this.checkStatusListEntry(cred?.credentialStatus);
  }

  private async checkStatusListEntry(credentialStatus: any): Promise<boolean | null> {
    if (!this.statusListEnabled || !credentialStatus) return null;
    const listId = credentialStatus.revocationListCredential;
    const index = parseInt(credentialStatus.revocationListIndex, 10);
    if (!listId || isNaN(index)) return null;
    return this.statusListService.isRevoked(listId, index);
  }

  async verifyCredentialById(credId: string) {
    // getting the credential from the db
    const stored =
      (await this.prisma.verifiableCredentials.findUnique({
      where: {
        id: credId,
      },
      select: {
        signed: true,
        status: true,
        format: true,
        enveloped: true,
      },
    })) as { signed: Verifiable<W3CCredential>; status: VCStatus; format?: string; enveloped?: string } | null;

    this.logger.debug('Fetched credntial from db to verify');

    // invalid request in case credential is not found
    if (!stored || (!stored.signed && !stored.enveloped)) {
      this.logger.error('Credential not found');
      throw new NotFoundException({ errors: ['Credential not found'] });
    }
    // Enveloped formats verify from the compact string, not the JSON envelope.
    if (stored.format && stored.format !== 'ldp_vc' && stored.enveloped) {
      return this.verifyCredential(stored.enveloped, stored.status);
    }
    return this.verifyCredential(stored.signed, stored.status);
  }

  async getSuite(verificationMethod: VerificationMethod, signatureType: string) {
    const supportedSignatures = {
      "Ed25519Signature2020": ["Ed25519VerificationKey2020", "JsonWebKey2020", "Ed25519VerificationKey2018"],
      "Ed25519Signature2018": ["Ed25519VerificationKey2018"],
      "RsaSignature2018": ["RsaVerificationKey2018"],
    };
    if(!(signatureType in supportedSignatures)) {
      throw new NotFoundException("Suite for signature type not found");
    }
    if(!supportedSignatures[signatureType].includes(verificationMethod?.type)) {
      throw new NotFoundException("Suite for verification type not found");
    }
    if(!this.map[verificationMethod?.type]) await this.init();
    if(!this.map[verificationMethod?.type]) throw new NotFoundException("Library not loaded");
    let keyPair = await this.map[verificationMethod?.type].from(verificationMethod);
    return new this.map[signatureType]({key: keyPair});
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

    const format: CredentialFormat = issueRequest.format || 'ldp_vc';
    const issuerId = (credInReq.issuer as any)?.id || (credInReq.issuer as string);

    // Optionally reserve a status-list index and embed credentialStatus
    // BEFORE signing so the proof covers it. Off by default → ldp_vc unchanged.
    let statusListCredential: string | undefined;
    let statusListIndex: number | undefined;
    if (this.statusListEnabled) {
      try {
        const allocation = await this.statusListService.allocateIndex(issuerId);
        statusListCredential = allocation.statusListCredential;
        statusListIndex = allocation.index;
        (credInReq as any).credentialStatus =
          this.statusListService.buildCredentialStatus(statusListCredential, statusListIndex);
      } catch (err) {
        this.logger.error('Error allocating status list index', err);
      }
    }

    // sign the credential in the requested format
    let signResult: SignResult;
    try {
      signResult = await this.credentialFormatService.signInFormat(
        credInReq,
        credInReq.issuer,
        format,
        {
          disclosable: issueRequest.disclosable,
          holderJwk: issueRequest.holderJwk,
          docType: issueRequest.docType,
          namespaces: issueRequest.namespaces,
        }
      );
    } catch (err) {
      this.logger.error('Error signing the credential', err);
      throw new InternalServerErrorException('Problem signing the credential');
    }
    this.logger.debug(`signed credential (${format})`);

    const signedObj = signResult.signed as any;
    // For ldp_vc the proof lives on the signed object; for enveloped formats
    // there is no detached proof to persist separately.
    const proof = format === 'ldp_vc' ? (signedObj.proof as Proof) : undefined;

    // TODO: add created by and updated by
    const newCred = await this.prisma.verifiableCredentials.create({
      data: {
        id: credInReq.id,
        type: credInReq.type,
        issuer: issuerId,
        issuanceDate: credInReq.issuanceDate,
        expirationDate: credInReq.expirationDate,
        subject: credInReq.credentialSubject as JwtCredentialSubject,
        subjectId: (credInReq.credentialSubject as JwtCredentialSubject).id,
        proof: proof,
        credential_schema: issueRequest.credentialSchemaId, //because they can't refer to the schema db from here through an ID
        signed: signedObj as object,
        format,
        enveloped: signResult.enveloped,
        statusListCredential,
        statusListIndex,
        tags: issueRequest.tags,
      },
    });

    if (!newCred) {
      this.logger.error('Problem saving credential to db');
      throw new InternalServerErrorException('Problem saving credential to db');
    }
    this.logger.debug('saved credential to db');

    // Enveloped formats return the raw compact string as the credential so a
    // wallet can store it directly; ldp_vc returns the JSON-LD object (as today).
    let res: any;
    if (format === 'ldp_vc') {
      res = newCred.signed;
      delete res['options'];
    } else {
      res = signResult.enveloped;
    }
    return {
      credential: res,
      credentialSchemaId: newCred.credential_schema,
      createdAt: newCred.updated_at.toISOString(),
      updatedAt: newCred.updated_at.toISOString(),
      createdBy: '',
      updatedBy: '',
      tags: newCred.tags,
      format,
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
      // Also flip the StatusList bit so verifiers relying on the published
      // bitstring (not the DB) see the revocation. Best-effort; the DB status
      // remains the source of truth for the by-id verify path.
      if (
        this.statusListEnabled &&
        (credential as any).statusListCredential != null &&
        (credential as any).statusListIndex != null
      ) {
        try {
          await this.statusListService.setRevoked(
            (credential as any).statusListCredential,
            (credential as any).statusListIndex,
            credential.issuer
          );
        } catch (err) {
          this.logger.error('Error updating status list on revoke', err);
        }
      }
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

  // Serves the signed StatusList/RevocationList2020 credential so verifiers can
  // check revocation without contacting the issuer's DB (P0.6).
  async getStatusListCredential(id: string) {
    const signed = await this.statusListService.getStatusListCredential(id);
    if (!signed) throw new NotFoundException('Status list credential not found');
    return signed;
  }

  async getRevocationList(
    issuerId: string,
    page = 1,
    limit= 1000,
  ){
    let revocationList: RevocationListDTO[]

    if (issuerId === "") {
      throw new InternalServerErrorException('Please provide a valid issuer ID');
    }
    
    try {
      revocationList = await this.prisma.verifiableCredentials.findMany({
        where: {
          issuer: issuerId,
          status: VCStatus.REVOKED,
        },
        select: {
          id: true,
          tags : true,
          issuer : true,
          issuanceDate: true
        },
        skip: (page -1) * limit,
        take: limit,
        orderBy: {
          issuanceDate: 'desc',
        },
      }); 
    } catch (error) {
      this.logger.error('Error fetching RevocationList');
      throw new InternalServerErrorException('Error fetching revocationList');
    }
    return revocationList 
  }
}
