const config = require('../../config/config');
const {publicKeyPem, privateKeyPem, signingKeyType, publicKeyBase58, privateKeyBase58} = require('../../config/keys');
const {setDocumentLoader, KeyType} = require('certificate-signer-library/signer');
const {generateCredentials, verifyCredentials} = require('../../src/services/signerService');

let signingConfig = {
    publicKeyPem: publicKeyPem,
    privateKeyPem: privateKeyPem,
    publicKeyBase58: publicKeyBase58,
    privateKeyBase58: privateKeyBase58,
    keyType: signingKeyType,
    REGISTRY_URL: config.REGISTRY_URL,

    CERTIFICATE_NAMESPACE: config.CERTIFICATE_NAMESPACE,
    CERTIFICATE_CONTROLLER_ID: config.CERTIFICATE_CONTROLLER_ID,
    CERTIFICATE_DID: config.CERTIFICATE_DID,
    CERTIFICATE_PUBKEY_ID: config.CERTIFICATE_PUBKEY_ID,
    CERTIFICATE_ISSUER: config.CERTIFICATE_ISSUER,
};
const customDocumentLoader = {};
setDocumentLoader(customDocumentLoader, signingConfig);

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
        "issuer": "did:issuer:authorizedIssuer#23",
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
        "issuer": "did:issuer:authorizedIssuer#23",
        // "date": "28-09-2021",
    }
    const signedData = await generateCredentials(entity, JSON.stringify(template));
    // const verifiedStatus = await verifyCredentials(signedData, KeyType.ED25519);
    const verifiedStatus = await verifyCredentials(signedData, KeyType.ED25519, "DaipNW4xaH2bh1XGNNdqjnSYyru3hLnUgTBSfSvmZ2hi");
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
        "issuer": "did:authorizedIssuer:23423#23",
        // "date": "28-09-2021",
    }
    const signedData = await generateCredentials(entity, JSON.stringify(template));
    signedData.credentialSubject.name = "XXYY"
    const verifiedStatus = await verifyCredentials(signedData, KeyType.ED25519);
    console.log(verifiedStatus)
    expect(verifiedStatus.verified).not.toBeTruthy();
});

test('Should verify credentials by RSA Algo', async () => {
    signingConfig.keyType = KeyType.RSA;
    setDocumentLoader(customDocumentLoader, signingConfig);
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
        "issuer": "did:authorizedIssuer:23423#23",
        // "date": "28-09-2021",
    }
    const signedData = await generateCredentials(entity, JSON.stringify(template));
    console.log(signedData)
    const verifiedStatus = await verifyCredentials(signedData, KeyType.RSA);
    console.log(verifiedStatus)
    expect(verifiedStatus.verified).toBeTruthy();
});

test('Should verify credentials with external publickey', async () => {
    const signedData = {"@context":["https://www.w3.org/2018/credentials/v1",
            {"name":"schema:name"}],
        "type":["VerifiableCredential"],
        "credentialSubject":{"type":"Person","name":"Tejash"},
        "issuanceDate":"2021-08-27T10:57:57.237Z",
        "issuer":"did:issuer:authorizedIssuer#23",
        "proof":{
            "type":"Ed25519Signature2018",
            "created":"2022-07-13T07:18:35Z",
            "verificationMethod":"did:authorizedSigner:123456789",
            "proofPurpose":"assertionMethod",
            "jws":"eyJhbGciOiJFZERTQSIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..OWfu7zX1WFI5bWYRAo7I40yjhdtmP4Tvn_0m74E_U7PvsYGBWi72MtbPotrn3jtwZw2qkWrSD4UF8WXSPnW0Bg"
    }};
    const verifiedStatus = await verifyCredentials(signedData, KeyType.ED25519, "DaipNW4xaH2bh1XGNNdqjnSYyru3hLnUgTBSfSvmZ2hi");
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
        "issuer": "did:issuer:skill-master#23",
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