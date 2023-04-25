import { Prisma, Status as PrismaStatus } from '@prisma/client';
import { PrismaService } from '../prisma.service';
import { VCSModelSchemaInterface } from '../types/VCModelSchema.interface';
import { CreateCredentialDTO } from './dto/create-credentials.dto';
type schemaResponse = {
    schema: {};
    tags: string[];
    status: PrismaStatus;
    createdAt: string;
    createdBy: string;
    updatedAt: string;
    updatedBy: string;
};
export declare class SchemaService {
    private readonly prisma;
    constructor(prisma: PrismaService);
    getCredentialSchema(userWhereUniqueInput: Prisma.VerifiableCredentialSchemaWhereUniqueInput): Promise<schemaResponse>;
    createCredentialSchema(createCredentialDto: CreateCredentialDTO): Promise<schemaResponse>;
    updateCredentialSchema(params: {
        where: Prisma.VerifiableCredentialSchemaWhereUniqueInput;
        data: VCSModelSchemaInterface;
    }): Promise<schemaResponse>;
    getSchemaByTags(tags: string[]): Promise<schemaResponse[]>;
}
export {};
