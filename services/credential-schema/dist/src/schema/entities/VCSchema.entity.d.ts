export declare class VCSchema {
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
}
