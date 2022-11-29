const {generateCredentials, verifyCredentials, KeyType} = require('../../src/services/signerService');

test('Should generate credentials', async () => {
    const entity = {
        identityDetails: {
            name: "Tejash"
        }
    };
    const template = {
        "@context": ["https://www.w3.org/2018/credentials/v1", {"name": "schema:name"}],
        "type": ["VerifiableCredential"],
        "credentialSubject": {
            type: "Person",
            name: "{{identityDetails.name}}"
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "did:authorizedIssuer:23423#21",
        // "date": "28-09-2021",
    }
    const signedData = await generateCredentials(entity, JSON.stringify(template));
    console.log(signedData)
    expect(signedData.proof.jws).not.toBeNull();
});

test('Should verify credentials', async () => {
    const entity = {
        identityDetails: {
            name: "Tejash"
        }
    };
    const template = {
        "@context": ["https://www.w3.org/2018/credentials/v1",
            {"name": "schema:name"}
        ],
        "type": ["VerifiableCredential"],
        "credentialSubject": {
            type: "Person",
            name: "{{identityDetails.name}}"
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "did:authorizedIssuer:23423#21",
        // "date": "28-09-2021",
    }
    const signedData = await generateCredentials(entity, JSON.stringify(template));
    // const verifiedStatus = await verifyCredentials(signedData, KeyType.ED25519);
    const verifiedStatus = await verifyCredentials(signedData, "DaipNW4xaH2bh1XGNNdqjnSYyru3hLnUgTBSfSvmZ2hi");
    console.log(verifiedStatus)
    expect(verifiedStatus.verified).toBeTruthy();
});

test('Should fail verifying credentials of modified data', async () => {
    const entity = {
        identityDetails: {
            name: "Tejash"
        }
    };
    const template = {
        "@context": ["https://www.w3.org/2018/credentials/v1", {"name": "schema:name"}],
        "type": ["VerifiableCredential"],
        "credentialSubject": {
            type: "Person",
            name: "{{identityDetails.name}}"
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "did:authorizedIssuer:23423#21",
        // "date": "28-09-2021",
    }
    const signedData = await generateCredentials(entity, JSON.stringify(template));
    signedData.credentialSubject.name = "XXYY"
    const verifiedStatus = await verifyCredentials(signedData);
    console.log(verifiedStatus)
    expect(verifiedStatus.verified).not.toBeTruthy();
});

test('Should verify credentials by RSA Algo', async () => {
    const entity = {
        identityDetails: {
            name: "Tejash"
        }
    };
    const publicKeyPem = '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnXQalrgztecTpc+INjRQ8s73FSE1kU5QSlwBdICCVJBUKiuQUt7s+Z5epgCvLVAOCbP1mm5lV7bfgV/iYWDio7lzX4MlJwDedWLiufr3Ajq+79CQiqPaIbZTo0i13zijKtX7wgxQ78wT/HkJRLkFpmGeK3za21tEfttytkhmJYlwaDTEc+Kx3RJqVhVh/dfwJGeuV4Xc/e2NH++ht0ENGuTk44KpQ+pwQVqtW7lmbDZQJoOJ7HYmmoKGJ0qt2hrj15uwcD1WEYfY5N7N0ArTzPgctExtZFDmituLGzuAZfv2AZZ9/7Y+igshzfB0reIFdUKw3cdVTzfv5FNrIqN5pwIDAQAB\n-----END PUBLIC KEY-----\n';
    const template = {
        "@context": ["https://www.w3.org/2018/credentials/v1", {"name": "schema:name"}],
        "type": ["VerifiableCredential"],
        "credentialSubject": {
            type: "Person",
            name: "{{identityDetails.name}}"
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "https://test.com/issuer",
        // "date": "28-09-2021",
    }
    const signedData = await generateCredentials(entity, JSON.stringify(template));
    console.log(signedData)
    const verifiedStatus = await verifyCredentials(signedData, publicKeyPem);
    console.log(verifiedStatus)
    expect(verifiedStatus.verified).toBeTruthy();
});

test('Should verify credentials with external publickey', async () => {
    const signedData = {"@context":["https://www.w3.org/2018/credentials/v1",{"name":"schema:name"}],"type":["VerifiableCredential"],"credentialSubject":{"type":"Person","name":"Tejash"},"issuanceDate":"2021-08-27T10:57:57.237Z","issuer":"did:authorizedIssuer:23423#21","proof":{"type":"Ed25519Signature2018","created":"2022-07-19T05:11:21Z","verificationMethod":"did:authorizedIssuer:23423#21","proofPurpose":"assertionMethod","jws":"eyJhbGciOiJFZERTQSIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..O6u5oAOiOeOt4nCHlJRRn04EyOoi7sRZHcJtvRXQqvKxQUfKdYjB9GJunO5pjnErWJC4U7gTctmUq34qrx98Bw"}};
    const verifiedStatus = await verifyCredentials(signedData, "DaipNW4xaH2bh1XGNNdqjnSYyru3hLnUgTBSfSvmZ2hi");
    console.log(verifiedStatus)
    expect(verifiedStatus.verified).toBeTruthy();
});

test('Should generate credentials with array values', async () => {
    const entity = {
        "name": "a",
        "id": "123",
        "skills": [{"skill":"a"},{"skill":"b"}]
    };
    const template = `{
        "@context": [
            "https://www.w3.org/2018/credentials/v1",
            {
                "@context": {
                    "@version": 1.1,
                    "@protected": true,
                    "SkillCertificate": {
                        "@id": "https://github.com/sunbird-specs/vc-specs#SkillCertificate",
                        "@context": {
                            "id": "@id",
                            "@version": 1.1,
                            "@protected": true,
                            "skills": {
                                "@id": "https://github.com/sunbird-specs/vc-specs#skills",
                                "@container": "@list"
                            },
                            "name":"schema:Text",
                            "refId":"schema:Text"
                        }
                    },
                    
                    "trainedOn": {
                        "@id": "https://github.com/sunbird-specs/vc-specs#trainedOn",
                        "@context": {
                            "name": "schema:Text"
                        }
                    }
                }
            }
        ],
        "type": [
            "VerifiableCredential","SkillCertificate"
        ],
        "credentialSubject": {
            "type": "SkillCertificate",
            "refId": "{{id}}",
            "name": "{{name}}",
            "skills": [{{#each skills}}"{{skill}}"{{#unless @last}},{{/unless}}{{/each}}]
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "did:authorizedIssuer:23423#21",
        "evidence": [
            {
                "type": [
                    "Vaccination"
                ],
                "id": "https://sunbird.org/id"
            }
        ]
    }`;
    const signedData = await generateCredentials(entity, template);
    console.log(signedData.credentialSubject.skills)
    expect(signedData.credentialSubject.skills.length).toEqual(2);
});

test('Should cache remote context urls and generate VC without error', async () => {
    const entity = {
        identityDetails: {
            "name": "Ade Hastuti",
            "account_number": "229899429"
        }
    };
    const template = {
        "@context": ["https://www.w3.org/2018/credentials/v1", "https://gist.githubusercontent.com/tejash-jl/181288c54e573304398de78b311d2055/raw/a655e82f0d99e9779c15dbc7d70f7d8b978df6f1/c2.json"],
        "type": ["VerifiableCredential"],
        "credentialSubject": {
            type: "Beneficiary",
            name: "{{identityDetails.name}}",
            "account_number": "{{account_number}}"
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "did:authorizedIssuer:23423#21",
        // "date": "28-09-2021",
    }
    let signedData = await generateCredentials(entity, JSON.stringify(template));
    console.log(signedData)
    expect(signedData.proof.jws).not.toBeNull();
    signedData = await generateCredentials(entity, JSON.stringify(template));
    console.log(signedData)
    expect(signedData.proof.jws).not.toBeNull();
});