"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.CredentialsService = void 0;
const axios_1 = require("@nestjs/axios");
const common_1 = require("@nestjs/common");
const client_1 = require("@prisma/client");
const did_jwt_vc_1 = require("did-jwt-vc");
const prisma_service_1 = require("../prisma.service");
const renderOutput_enum_1 = require("./enums/renderOutput.enum");
const handlebars_1 = require("handlebars");
const wkhtmltopdf = require("wkhtmltopdf");
const QRCode = require('qrcode');
const ION = require('@decentralized-identity/ion-tools');
let CredentialsService = class CredentialsService {
    constructor(prisma, httpService) {
        this.prisma = prisma;
        this.httpService = httpService;
    }
    async getCredentials(tags) {
        const credentials = await this.prisma.vCV2.findMany({
            where: {
                tags: {
                    hasSome: [...tags],
                },
            },
        });
        return credentials;
    }
    async getCredentialById(id) {
        const credential = await this.prisma.vCV2.findUnique({
            where: { id: id },
            select: {
                signed: true,
            },
        });
        if (!credential)
            throw new common_1.NotFoundException('Credential for the given id not found');
        const res = credential.signed;
        delete res['options'];
        delete res['proof'];
        res['id'] = id;
        return res;
    }
    async verifyCredential(credId) {
        var _a;
        let credToVerify = null;
        credToVerify = await this.prisma.vCV2.findUnique({
            where: {
                id: credId,
            },
        });
        if (!credToVerify) {
            throw new common_1.NotFoundException({ errors: ['Credential not found'] });
        }
        try {
            const status = credToVerify.status;
            credToVerify = credToVerify.signed;
            delete credToVerify['options'];
            console.log('process.env.IDENTIY_BASE_URL: ', process.env.IDENTITY_BASE_URL);
            const verificationMethod = credToVerify.issuer;
            const verificationURL = `${process.env.IDENTITY_BASE_URL}/did/resolve/${verificationMethod}`;
            console.log('verificationURL: ', verificationURL);
            const dIDResponse = await this.httpService.axiosRef.get(verificationURL);
            const did = dIDResponse.data;
            const verified = await ION.verifyJws({
                jws: (_a = credToVerify === null || credToVerify === void 0 ? void 0 : credToVerify.proof) === null || _a === void 0 ? void 0 : _a.proofValue,
                publicJwk: did.verificationMethod[0].publicKeyJwk,
            });
            return {
                status: status,
                checks: [
                    {
                        active: 'OK',
                        revoked: status === client_1.VCStatus.REVOKED ? 'NOK' : 'OK',
                        expired: new Date(credToVerify.expirationDate).getTime() < Date.now()
                            ? 'NOK'
                            : 'OK',
                        proof: 'OK',
                    },
                ],
            };
        }
        catch (e) {
            console.error(e);
            return {
                errors: [e],
            };
        }
    }
    async signVC(credentialPlayload, did) {
        const signedVCResponse = await this.httpService.axiosRef.post(`${process.env.IDENTITY_BASE_URL}/utils/sign`, {
            DID: did,
            payload: JSON.stringify(credentialPlayload),
        });
        return signedVCResponse.data.signed;
    }
    async issueCredential(issueRequest) {
        var _a;
        try {
            const credInReq = issueRequest.credential;
            credInReq['proof'] = {
                proofValue: await this.signVC((0, did_jwt_vc_1.transformCredentialInput)(credInReq), credInReq.issuer),
                type: 'Ed25519Signature2020',
                created: new Date().toISOString(),
                verificationMethod: credInReq.issuer,
                proofPurpose: 'assertionMethod',
            };
            console.timeEnd('Sign');
            const id = await this.httpService.axiosRef.post(`${process.env.IDENTITY_BASE_URL}/did/generate`, {
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
            });
            credInReq.id = (_a = id.data[0]) === null || _a === void 0 ? void 0 : _a.id;
            const newCred = await this.prisma.vCV2.create({
                data: {
                    id: credInReq.id,
                    type: credInReq.type,
                    issuer: credInReq.issuer,
                    issuanceDate: credInReq.issuanceDate,
                    expirationDate: credInReq.expirationDate,
                    subject: credInReq.credentialSubject,
                    subjectId: credInReq.credentialSubject.id,
                    proof: credInReq.proof,
                    credential_schema: issueRequest.credentialSchemaId,
                    signed: credInReq,
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
        }
        catch (err) {
            throw new common_1.InternalServerErrorException(err);
        }
    }
    async deleteCredential(id) {
        try {
            const credential = await this.prisma.vCV2.update({
                where: { id: id },
                data: {
                    status: 'REVOKED',
                },
            });
            return credential;
        }
        catch (err) {
            throw new common_1.InternalServerErrorException(err);
        }
    }
    async getCredentialsBySubjectOrIssuer(getCreds) {
        var _a;
        try {
            const filteringSubject = getCreds.subject;
            const credentials = await this.prisma.vCV2.findMany({
                where: {
                    issuer: (_a = getCreds.issuer) === null || _a === void 0 ? void 0 : _a.id,
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
                throw new common_1.NotFoundException('No credentials found for the given subject or issuer');
            return credentials.map((cred) => {
                const signed = cred.signed;
                delete signed['id'];
                delete signed['options'];
                delete signed['proof'];
                return Object.assign({ id: cred.id }, signed);
            });
        }
        catch (err) {
            throw new common_1.InternalServerErrorException(err);
        }
    }
    async renderCredential(renderingRequest) {
        const output = renderingRequest.output;
        const rendering_template = renderingRequest.template;
        const credential = renderingRequest.credential;
        const subject = credential.credentialSubject;
        subject.qr = await this.renderAsQR(credential);
        console.log(subject);
        const template = (0, handlebars_1.compile)(rendering_template);
        const data = template(subject);
        delete subject.id;
        switch (output) {
            case renderOutput_enum_1.RENDER_OUTPUT.QR:
                const QRData = await this.renderAsQR(credential);
                console.log(QRData);
                return QRData;
                break;
            case renderOutput_enum_1.RENDER_OUTPUT.STRING:
                break;
            case renderOutput_enum_1.RENDER_OUTPUT.PDF:
                return wkhtmltopdf(data, {
                    pageSize: 'A4',
                    disableExternalLinks: true,
                    disableInternalLinks: true,
                    disableJavascript: true,
                    encoding: 'UTF-8',
                });
            case renderOutput_enum_1.RENDER_OUTPUT.QR_LINK:
                return data;
                break;
            case renderOutput_enum_1.RENDER_OUTPUT.HTML:
                return data;
                break;
            case renderOutput_enum_1.RENDER_OUTPUT.STRING:
                break;
            case renderOutput_enum_1.RENDER_OUTPUT.JSON:
                break;
        }
    }
    async renderAsQR(cred) {
        try {
            const verificationURL = `http://64.227.185.154:3002/credentials/${cred.id}/verify`;
            const QRData = await QRCode.toDataURL(verificationURL);
            return QRData;
        }
        catch (err) {
            console.error(err);
            return err;
        }
    }
    async getSchemaByCredId(credId) {
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
        }
        catch (e) {
            console.log(e);
            throw e;
        }
    }
};
CredentialsService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        axios_1.HttpService])
], CredentialsService);
exports.CredentialsService = CredentialsService;
//# sourceMappingURL=credentials.service.js.map