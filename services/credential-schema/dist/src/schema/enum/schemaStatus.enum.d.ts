export declare const schemaStatus: {
    [x: string]: 'REVOKED' | 'DRAFT' | 'PUBLISHED';
};
export type schemaStatus = typeof schemaStatus[keyof typeof schemaStatus];
