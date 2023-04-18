import { CredentialsService } from './credentials.service';
import { GetCredentialsBySubjectOrIssuer } from './dto/getCredentialsBySubjectOrIssuer.dto';
import { IssueCredentialDTO } from './dto/issue-credential.dto';
import { RenderTemplateDTO } from './dto/renderTemplate.dto';
import { Response } from 'express';
export declare class CredentialsController {
    private readonly credentialsService;
    constructor(credentialsService: CredentialsService);
    getCredentials(tags: string): Promise<import(".prisma/client").VCV2[]>;
    getCredentialsBySubject(getCreds: GetCredentialsBySubjectOrIssuer): Promise<{
        id: string;
    }[]>;
    getCredentialById(id: {
        id: string;
    }): Promise<import(".prisma/client").Prisma.JsonValue>;
    issueCredentials(issueRequest: IssueCredentialDTO): Promise<{
        credential: import(".prisma/client").Prisma.JsonValue;
        credentialSchemaId: string;
        createdAt: Date;
        updatedAt: Date;
        createdBy: string;
        updatedBy: string;
        tags: string[];
    }>;
    delteCredential(id: string): Promise<import(".prisma/client").VCV2>;
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
    renderTemplate(renderRequest: RenderTemplateDTO, response: Response): Promise<any>;
    getSchemaByCredId(id: string): Promise<{
        credential_schema: string;
    }>;
}
