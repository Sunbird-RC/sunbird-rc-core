
describe('environmental variables', () => {
    beforeEach(() => {
        jest.resetModules() // Most important - it clears the cache
        process.env.CUSTOM_TEMPLATE_DELIMITERS = '\\${,}';
        // const {setDocumentLoader, KeyType} = require('certificate-signer-library/signer');
        // const {publicKeyPem, privateKeyPem, signingKeyType, publicKeyBase58, privateKeyBase58} = require('../../config/keys');
        // const {vaccinationContext} = require("vaccination-context");
        // const config = require('../../config/config');
        // let signingConfig = {
        //     publicKeyPem: publicKeyPem,
        //     privateKeyPem: privateKeyPem,
        //     publicKeyBase58: publicKeyBase58,
        //     privateKeyBase58: privateKeyBase58,
        //     keyType: signingKeyType,
        //     REGISTRY_URL: config.REGISTRY_URL,
        //
        //     CERTIFICATE_CONTROLLER_ID: config.CERTIFICATE_CONTROLLER_ID,
        //     CERTIFICATE_DID: config.CERTIFICATE_DID,
        //     CERTIFICATE_PUBKEY_ID: config.CERTIFICATE_PUBKEY_ID,
        //     CERTIFICATE_ISSUER: config.CERTIFICATE_ISSUER,
        // };
        const customDocumentLoader = {};
        // setDocumentLoader(customDocumentLoader, signingConfig);

    });

    test('Should generate credentials with different delimiter', async () => {
        const {generateCredentials, verifyCredentials} = require('../../src/services/signerService');
        const config = require('../../config/config');

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
                name: "${identityDetails.name}"
            },
            "issuanceDate": "2021-08-27T10:57:57.237Z",
            "issuer": "did:authorizedIssuer:23423#21",
            // "date": "28-09-2021",
        }
        const signedData = await generateCredentials(entity, JSON.stringify(template));
        console.log(signedData)
        expect(signedData.credentialSubject.name).toEqual(entity.identityDetails.name);
    });

});