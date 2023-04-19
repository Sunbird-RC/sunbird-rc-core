import { VCSModelSchemaInterface } from 'src/types/VCModelSchema.interface';
import { Status as PrismaStatus } from '@prisma/client';
export declare class VCModelSchema implements VCSModelSchemaInterface {
    schema: {
        type: string;
        version: string;
        id: string;
        name: string;
        author: string;
        authored: string;
        schema: {
            $id: string;
            $schema: string;
            description: string;
            name?: string;
            type: string;
            properties: {
                [k: string]: unknown;
            };
            required: [] | [string];
            additionalProperties: boolean;
            [k: string]: unknown;
        };
    };
    tags: [] | [string];
    status: PrismaStatus;
}
