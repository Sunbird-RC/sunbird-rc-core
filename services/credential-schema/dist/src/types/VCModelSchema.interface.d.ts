import { Status } from "@prisma/client";
export interface VCSModelSchemaInterface {
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
        };
    };
    tags: [] | [string];
    status: Status;
}
