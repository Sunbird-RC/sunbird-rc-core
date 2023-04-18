import { ConfigService } from '@nestjs/config';
import { IssueRequest, VCRequest } from './app.interface';
import { PrismaService } from './prisma.service';
import { VC } from '@prisma/client';
import { W3CCredential, Verifiable, JwtCredentialPayload } from 'did-jwt-vc';
import { HttpService } from '@nestjs/axios';
export declare class AppService {
    private prisma;
    private configService;
    private httpService;
    constructor(prisma: PrismaService, configService: ConfigService, httpService: HttpService);
    claim(vcReqestData: VCRequest): Promise<VC>;
    signVC(credentialPlayload: JwtCredentialPayload, did: string): Promise<string>;
    issue(vcReqestData: IssueRequest): Promise<VC>;
    getVCBySubject(sub: string): import(".prisma/client").PrismaPromise<{
        signed: import(".prisma/client").Prisma.JsonValue;
    }[]>;
    getVCByIssuer(issuer: string): import(".prisma/client").PrismaPromise<{
        signed: import(".prisma/client").Prisma.JsonValue;
    }[]>;
    verify(credential: Verifiable<W3CCredential>): Promise<boolean>;
    renderAsQR(credentialId: string): Promise<any>;
}
