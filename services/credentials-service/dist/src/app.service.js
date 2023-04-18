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
exports.AppService = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const prisma_service_1 = require("./prisma.service");
const did_jwt_vc_1 = require("did-jwt-vc");
const axios_1 = require("@nestjs/axios");
const ION = require('@decentralized-identity/ion-tools');
const QRCode = require('qrcode');
let AppService = class AppService {
    constructor(prisma, configService, httpService) {
        this.prisma = prisma;
        this.configService = configService;
        this.httpService = httpService;
    }
    async claim(vcReqestData) {
        const unsignedVC = await this.prisma.vC.create({
            data: {
                subject: vcReqestData.subject,
                type: vcReqestData.type,
                issuer: vcReqestData.issuer,
                unsigned: vcReqestData.credential,
                credential_schema: vcReqestData.schema,
            },
        });
        return unsignedVC;
    }
    async signVC(credentialPlayload, did) {
        console.time("Axios Resp");
        const signedVCResponse = await this.httpService.axiosRef.post(`${process.env.IDENTITY_BASE_URL}/utils/sign`, {
            DID: did,
            payload: JSON.stringify(credentialPlayload),
        });
        console.timeEnd("Axios Resp");
        return signedVCResponse.data.signed;
    }
    async issue(vcReqestData) {
        const credential = await this.prisma.vC.findUnique({
            where: { id: vcReqestData.id },
        });
        const signedCredential = credential.unsigned;
        signedCredential.proof = {
            proofValue: await this.signVC((0, did_jwt_vc_1.transformCredentialInput)(credential.unsigned), credential.issuer),
            type: 'Ed25519Signature2020',
            created: new Date().toISOString(),
            verificationMethod: credential.issuer,
            proofPurpose: 'assertionMethod',
        };
        const signedVC = await this.prisma.vC.update({
            data: {
                signed: signedCredential,
            },
            where: { id: vcReqestData.id },
        });
        return signedVC;
    }
    getVCBySubject(sub) {
        return this.prisma.vC.findMany({
            where: {
                subject: sub,
            },
            select: {
                signed: true,
            },
        });
    }
    getVCByIssuer(issuer) {
        return this.prisma.vC.findMany({
            where: {
                issuer: issuer,
            },
            select: {
                signed: true,
            },
        });
    }
    async verify(credential) {
        const verificationMethod = 'did:ulp:5d7682f4-3cca-40fb-9fa2-1f6ebef4803b';
        const dIDResponse = await this.httpService.axiosRef.get(`${process.env.IDENTITY_BASE_URL}/did/resolve/${verificationMethod}`);
        const did = dIDResponse.data;
        try {
            const verified = await ION.verifyJws({
                jws: credential.proof.proofValue,
                publicJwk: did.verificationMethod[0].publicKeyJwk,
            });
            console.debug(verified);
            return true;
        }
        catch (e) {
            return false;
        }
    }
    async renderAsQR(credentialId) {
        const credential = await this.prisma.vCV2.findUnique({
            where: { id: credentialId },
        });
        try {
            const QRData = await QRCode.toDataURL(credential.signed.proof.proofValue);
            return QRData;
        }
        catch (err) {
            console.error(err);
            return err;
        }
    }
};
AppService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        config_1.ConfigService,
        axios_1.HttpService])
], AppService);
exports.AppService = AppService;
//# sourceMappingURL=app.service.js.map