const {vaccinationContext} = require("vaccination-context");
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
customDocumentLoader[config.CERTIFICATE_NAMESPACE] = vaccinationContext;
setDocumentLoader(customDocumentLoader, signingConfig);

test('Should generate credentials', async () => {
    const entity = {
        identityDetails: {
            name: "Tejash"
        }
    };
    const template = {
        "@context": ["https://www.w3.org/2018/credentials/v1", config.CERTIFICATE_NAMESPACE],
        "type": ["VerifiableCredential"],
        "credentialSubject": {
            type: "Person",
            name: "{{identityDetails.name}}"
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "did:issuer:cowin",
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
        "@context": ["https://www.w3.org/2018/credentials/v1", config.CERTIFICATE_NAMESPACE],
        "type": ["VerifiableCredential"],
        "credentialSubject": {
            type: "Person",
            name: "{{identityDetails.name}}"
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "did:issuer:cowin",
        // "date": "28-09-2021",
    }
    const signedData = await generateCredentials(entity, JSON.stringify(template));
    const verifiedStatus = await verifyCredentials(signedData, KeyType.ED25519);
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
        "@context": ["https://www.w3.org/2018/credentials/v1", config.CERTIFICATE_NAMESPACE],
        "type": ["VerifiableCredential"],
        "credentialSubject": {
            type: "Person",
            name: "{{identityDetails.name}}"
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "did:issuer:cowin",
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
        "@context": ["https://www.w3.org/2018/credentials/v1", config.CERTIFICATE_NAMESPACE],
        "type": ["VerifiableCredential"],
        "credentialSubject": {
            type: "Person",
            name: "{{identityDetails.name}}"
        },
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "issuer": "did:issuer:cowin",
        // "date": "28-09-2021",
    }
    const signedData = await generateCredentials(entity, JSON.stringify(template));
    console.log(signedData)
    const verifiedStatus = await verifyCredentials(signedData, KeyType.RSA);
    console.log(verifiedStatus)
    expect(verifiedStatus.verified).toBeTruthy();
});

test('Should verify credentials with external publickey', async () => {
    const signedData = {
        '@context': [
            'https://www.w3.org/2018/credentials/v1',
            'https://sunbird.org/credentials/vaccination/v1'
        ],
        type: [ 'VerifiableCredential' ],
        credentialSubject: { type: 'Person', name: 'Tejash' },
        issuanceDate: '2021-08-27T10:57:57.237Z',
        issuer: 'did:issuer:cowin',
        proof: {
            type: 'Ed25519Signature2018',
            created: '2021-12-08T08:34:39Z',
            verificationMethod: 'did:india',
            proofPurpose: 'assertionMethod',
            jws: 'eyJhbGciOiJFZERTQSIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..fJlWEsijhIsAV8cc2UOCndoEaDJRP14L7orcFstn9f8uzFXqi-jvC-r6g58FQdwc99ZmeYpgy94wBDCuVKj0BQ'
        }
    }
    const verifiedStatus = await verifyCredentials(signedData, KeyType.ED25519, "7mQq3uQuK3AL5mPhhCKEHXpdMSHCRMtZntxMMn7YQrY3");
    console.log(verifiedStatus)
    expect(verifiedStatus.verified).toBeTruthy();
});