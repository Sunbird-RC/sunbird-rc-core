import { Status as PrismaStatus } from '@prisma/client';
import { Cache } from 'cache-manager';
import { VCSModelSchemaInterface } from 'src/types/VCModelSchema.interface';
import { CreateCredentialDTO } from './dto/create-credentials.dto';
import { SchemaService } from './schema.service';
type schemaResponse = {
    schema: {};
    tags: string[];
    status: PrismaStatus;
    createdAt: string;
    createdBy: string;
    updatedAt: string;
    updatedBy: string;
};
export declare class SchemaController {
    private readonly schemaService;
    private cacheManager;
    constructor(schemaService: SchemaService, cacheManager: Cache);
    getCredentialSchema(id: string): Promise<{
        schema: {};
        tags: string[];
        status: PrismaStatus;
        createdAt: string;
        createdBy: string;
        updatedAt: string;
        updatedBy: string;
    }>;
    getCredentialSchemaByTags(tags: string): Promise<{
        schema: {};
        tags: string[];
        status: PrismaStatus;
        createdAt: string;
        createdBy: string;
        updatedAt: string;
        updatedBy: string;
    }[]>;
    createCredentialSchema(body: CreateCredentialDTO): Promise<schemaResponse>;
    updateCredentialSchema(id: any, data: VCSModelSchemaInterface): Promise<{
        schema: {};
        tags: string[];
        status: PrismaStatus;
        createdAt: string;
        createdBy: string;
        updatedAt: string;
        updatedBy: string;
    }>;
}
export {};
