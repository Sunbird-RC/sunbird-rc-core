export declare const samples: ({
    sample: {
        '@context': string[];
        type: string[];
        issuer: {
            id: string;
        };
        name: string;
        description: string;
        issuanceDate: string;
        credentialSubject: {
            id: string;
            alumniOf: {
                identifier: string;
                name: string;
            };
        };
        id: string;
        proof: {
            type: string;
            created: string;
            verificationMethod: string;
            proofPurpose: string;
            proofValue: string;
        };
        version?: undefined;
        author?: undefined;
        authored?: undefined;
        schema?: undefined;
    };
    isValid: boolean;
} | {
    sample: {
        type: string;
        version: string;
        id: string;
        name: string;
        author: string;
        authored: string;
        '@context'?: undefined;
        issuer?: undefined;
        description?: undefined;
        issuanceDate?: undefined;
        credentialSubject?: undefined;
        proof?: undefined;
        schema?: undefined;
    };
    isValid: boolean;
} | {
    sample: {
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
            type: string;
            properties: {
                emailAddress: {
                    type: string;
                    format: string;
                };
                backupEmailAddress: {
                    type: string;
                    format: string;
                };
                alumniOf?: undefined;
                grade?: undefined;
                programme?: undefined;
                certifyingInstitute?: undefined;
                evaluatingInstitute?: undefined;
                skill?: undefined;
                trainingInstitute?: undefined;
                score?: undefined;
                examinationName?: undefined;
                releasingInstitution?: undefined;
                organisingInstitute?: undefined;
            };
            required: string[];
            additionalProperties: boolean;
        };
        '@context'?: undefined;
        issuer?: undefined;
        description?: undefined;
        issuanceDate?: undefined;
        credentialSubject?: undefined;
        proof?: undefined;
    };
    isValid: boolean;
} | {
    sample: {
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
            type: string;
            properties: {
                emailAddress: {
                    type: string;
                    format: string;
                };
                backupEmailAddress?: undefined;
                alumniOf?: undefined;
                grade?: undefined;
                programme?: undefined;
                certifyingInstitute?: undefined;
                evaluatingInstitute?: undefined;
                skill?: undefined;
                trainingInstitute?: undefined;
                score?: undefined;
                examinationName?: undefined;
                releasingInstitution?: undefined;
                organisingInstitute?: undefined;
            };
            required: string[];
            additionalProperties: boolean;
        };
        '@context'?: undefined;
        issuer?: undefined;
        description?: undefined;
        issuanceDate?: undefined;
        credentialSubject?: undefined;
        proof?: undefined;
    };
    isValid: boolean;
} | {
    sample: {
        '@context': string[];
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
            type: string;
            properties: {
                alumniOf: {
                    type: string;
                    properties: {
                        identifier: {
                            type: string;
                            format: string;
                            description: string;
                        };
                        name: {
                            type: string;
                            description: string;
                        };
                    };
                    required: string[];
                };
                emailAddress?: undefined;
                backupEmailAddress?: undefined;
                grade?: undefined;
                programme?: undefined;
                certifyingInstitute?: undefined;
                evaluatingInstitute?: undefined;
                skill?: undefined;
                trainingInstitute?: undefined;
                score?: undefined;
                examinationName?: undefined;
                releasingInstitution?: undefined;
                organisingInstitute?: undefined;
            };
            required: string[];
            additionalProperties: boolean;
        };
        proof: {
            type: string;
            created: string;
            verificationMethod: string;
            proofPurpose: string;
            proofValue: string;
        };
        issuer?: undefined;
        description?: undefined;
        issuanceDate?: undefined;
        credentialSubject?: undefined;
    };
    isValid: boolean;
} | {
    sample: {
        '@context': string[];
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
            type: string;
            properties: {
                grade: {
                    type: string;
                    description: string;
                };
                programme: {
                    type: string;
                    description: string;
                };
                certifyingInstitute: {
                    type: string;
                    description: string;
                };
                evaluatingInstitute: {
                    type: string;
                    description: string;
                };
                emailAddress?: undefined;
                backupEmailAddress?: undefined;
                alumniOf?: undefined;
                skill?: undefined;
                trainingInstitute?: undefined;
                score?: undefined;
                examinationName?: undefined;
                releasingInstitution?: undefined;
                organisingInstitute?: undefined;
            };
            required: string[];
            additionalProperties: boolean;
        };
        proof: {
            type: string;
            created: string;
            verificationMethod: string;
            proofPurpose: string;
            proofValue: string;
        };
        issuer?: undefined;
        description?: undefined;
        issuanceDate?: undefined;
        credentialSubject?: undefined;
    };
    isValid: boolean;
} | {
    sample: {
        '@context': string[];
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
            type: string;
            properties: {
                skill: {
                    type: string;
                    description: string;
                };
                certifyingInstitute: {
                    type: string;
                    description: string;
                };
                trainingInstitute: {
                    type: string;
                    description: string;
                };
                emailAddress?: undefined;
                backupEmailAddress?: undefined;
                alumniOf?: undefined;
                grade?: undefined;
                programme?: undefined;
                evaluatingInstitute?: undefined;
                score?: undefined;
                examinationName?: undefined;
                releasingInstitution?: undefined;
                organisingInstitute?: undefined;
            };
            required: string[];
            additionalProperties: boolean;
        };
        proof: {
            type: string;
            created: string;
            verificationMethod: string;
            proofPurpose: string;
            proofValue: string;
        };
        issuer?: undefined;
        description?: undefined;
        issuanceDate?: undefined;
        credentialSubject?: undefined;
    };
    isValid: boolean;
} | {
    sample: {
        '@context': string[];
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
            type: string;
            properties: {
                score: {
                    type: string;
                    description: string;
                };
                examinationName: {
                    type: string;
                    description: string;
                };
                releasingInstitution: {
                    type: string;
                    description: string;
                };
                organisingInstitute: {
                    type: string;
                    description: string;
                };
                emailAddress?: undefined;
                backupEmailAddress?: undefined;
                alumniOf?: undefined;
                grade?: undefined;
                programme?: undefined;
                certifyingInstitute?: undefined;
                evaluatingInstitute?: undefined;
                skill?: undefined;
                trainingInstitute?: undefined;
            };
            required: string[];
            additionalProperties: boolean;
        };
        proof: {
            type: string;
            created: string;
            verificationMethod: string;
            proofPurpose: string;
            proofValue: string;
        };
        issuer?: undefined;
        description?: undefined;
        issuanceDate?: undefined;
        credentialSubject?: undefined;
    };
    isValid: boolean;
})[];
