import { Prisma, VerifiableCredentialSchema, Status as PrismaStatus } from '@prisma/client';
export declare class VCItem implements VerifiableCredentialSchema {
    id: string;
    name: string;
    description: string;
    version: string;
    type: string;
    author: string;
    authored: Date;
    schema: Prisma.JsonValue;
    proof: Prisma.JsonValue;
    createdAt: Date;
    updatedAt: Date;
    createdBy: string;
    updatedBy: string;
    deletedAt: Date | null;
    tags: string[];
    status: PrismaStatus;
}
