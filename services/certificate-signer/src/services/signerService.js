const Mustache = require('mustache');
const {signJSON} = require('certificate-signer-library');
const {customLoader, KeyType} = require('certificate-signer-library/signer');
const jsigs = require('jsonld-signatures');
const {Ed25519KeyPair, RSAKeyPair} = require('crypto-ld');
const {Ed25519Signature2018, RsaSignature2018} = jsigs.suites;
const {publicKeyPem, publicKeyBase58} = require('../../config/keys');
const {CERTIFICATE_DID, CERTIFICATE_CONTROLLER_ID} = require('../../config/config');
const vc = require('vc-js');

const generateCredentials = async (data, credentialTemplate = "") => {
    console.log("Input received", credentialTemplate, data);
    let renderedTemplate = Mustache.render(credentialTemplate, data);
    const credentialData = JSON.parse(renderedTemplate);
    console.log("Sending", credentialData);
    return await signJSON(credentialData);
};

const getPublicKey = (signingKeyType, publicKey = null) => {
    switch (signingKeyType) {
        case KeyType.RSA:
            return {
                '@context': jsigs.SECURITY_CONTEXT_URL,
                id: CERTIFICATE_DID,
                type: 'RsaVerificationKey2018',
                controller: CERTIFICATE_CONTROLLER_ID,
                publicKeyPem: publicKey || publicKeyPem
            };
        case KeyType.ED25519:
            return {
                '@context': jsigs.SECURITY_CONTEXT_URL,
                id: CERTIFICATE_DID,
                type: 'Ed25519VerificationKey2018',
                controller: CERTIFICATE_CONTROLLER_ID,
                publicKeyBase58: publicKey || publicKeyBase58
            };
    }
};

const verifyCredentials = async (signedCredentials, signingKeyType, externalPublicKey=null) => {
    const publicKey = getPublicKey(signingKeyType, externalPublicKey);
    const controller = {
        '@context': jsigs.SECURITY_CONTEXT_URL,
        id: CERTIFICATE_CONTROLLER_ID,
        publicKey: [publicKey],
        // this authorizes this key to be used for making assertions
        assertionMethod: [publicKey.id]
    };
    switch (signingKeyType) {
        case KeyType.RSA:
            return await verifyRSACredentials(controller, signedCredentials, signingKeyType, externalPublicKey);
        case KeyType.ED25519:
            return await verifyED25519Credentials(controller, signedCredentials, signingKeyType, externalPublicKey);
    }
    console.log(result);
    return result;
};

const verifyRSACredentials = async (controller, signedCredentials, signingKeyType, externalPublicKey) => {
    const key = new RSAKeyPair({...getPublicKey(signingKeyType, externalPublicKey)});
    const {AssertionProofPurpose} = jsigs.purposes;
    return await jsigs.verify(signedCredentials, {
        suite: new RsaSignature2018({key}),
        purpose: new AssertionProofPurpose({controller}),
        compactProof: false,
        documentLoader: customLoader
    });
};


async function verifyED25519Credentials(controller, signedCredentials, signingKeyType, externalPublicKey) {
    const key = new Ed25519KeyPair({...getPublicKey(signingKeyType, externalPublicKey)});
    const {AssertionProofPurpose} = jsigs.purposes;
    const purpose = new AssertionProofPurpose({
        controller: controller
    });
    return await vc.verifyCredential({
        credential: signedCredentials,
        suite: new Ed25519Signature2018({key}),
        purpose: purpose,
        documentLoader: customLoader,
        compactProof: false
    });
}

module.exports = {
    generateCredentials,
    verifyCredentials
};