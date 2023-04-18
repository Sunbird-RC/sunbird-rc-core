import { HttpService } from '@nestjs/axios';
import { VCV2 } from '@prisma/client';
import { JwtCredentialPayload } from 'did-jwt-vc';
import { PrismaService } from '../prisma.service';
import { GetCredentialsBySubjectOrIssuer } from './dto/getCredentialsBySubjectOrIssuer.dto';
import { IssueCredentialDTO } from './dto/issue-credential.dto';
import { RenderTemplateDTO } from './dto/renderTemplate.dto';
export declare class CredentialsService {
    private readonly prisma;
    private readonly httpService;
    constructor(prisma: PrismaService, httpService: HttpService);
    getCredentials(tags: string[]): Promise<VCV2[]>;
    getCredentialById(id: string): Promise<import(".prisma/client").Prisma.JsonValue>;
    verifyCredential(credId: string): Promise<{
        status: any;
        checks: {
            active: string;
            revoked: string;
            expired: string;
            proof: string;
        }[];
        errors?: undefined;
    } | {
        errors: any[];
        status?: undefined;
        checks?: undefined;
    }>;
    signVC(credentialPlayload: JwtCredentialPayload, did: string): Promise<string>;
    issueCredential(issueRequest: IssueCredentialDTO): Promise<{
        credential: import(".prisma/client").Prisma.JsonValue;
        credentialSchemaId: string;
        createdAt: Date;
        updatedAt: Date;
        createdBy: string;
        updatedBy: string;
        tags: string[];
    }>;
    deleteCredential(id: string): Promise<VCV2>;
    getCredentialsBySubjectOrIssuer(getCreds: GetCredentialsBySubjectOrIssuer): Promise<{
        id: string;
    }[]>;
    renderCredential(renderingRequest: RenderTemplateDTO): Promise<any>;
    renderAsQR(cred: VCV2): Promise<any>;
    getSchemaByCredId(credId: string): Promise<{
        credential_schema: string;
    }>;
}
