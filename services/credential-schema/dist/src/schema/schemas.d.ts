declare const schemas: {
    proof_of_academic_evaluation: {
        "@context": string[];
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
    };
    proof_of_alumni: {
        "@context": string[];
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
    };
    proof_of_marks: {
        "@context": string[];
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
    };
    proof_of_training: {
        "@context": string[];
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
    };
    marksheet: {
        "@context": string[];
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
                roll_number: {
                    type: string;
                    description: string;
                };
                name: {
                    type: string;
                    description: string;
                };
                fathers_name: {
                    type: string;
                    description: string;
                };
                mothers_name: {
                    type: string;
                    description: string;
                };
                DOB: {
                    type: string;
                    description: string;
                };
                result: {
                    type: string;
                    description: string;
                };
                score_details: {
                    type: string;
                    description: string;
                    items: {
                        type: string;
                        properties: {
                            subject: {
                                type: string;
                                description: string;
                            };
                            theory_score: {
                                type: string;
                                description: string;
                            };
                            practical_score: {
                                type: string;
                                description: string;
                            };
                            total_score: {
                                type: string;
                                description: string;
                            };
                            grade: {
                                type: string;
                                description: string;
                            };
                        };
                        required: string[];
                    };
                };
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
    };
};
export default schemas;
