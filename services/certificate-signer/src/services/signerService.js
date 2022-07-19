const Mustache = require('mustache');
// const {signJSON} = require('certificate-signer-library');
//const {customLoader, KeyType} = require('certificate-signer-library/signer');
const jsigs = require('jsonld-signatures');
const {Ed25519KeyPair, RSAKeyPair} = require('crypto-ld');
const {Ed25519Signature2018, RsaSignature2018} = jsigs.suites;
const {AssertionProofPurpose} = jsigs.purposes;
const {publicKeyPem, publicKeyBase58} = require('../../config/keys');
const {CERTIFICATE_DID, CERTIFICATE_CONTROLLER_ID, CUSTOM_TEMPLATE_DELIMITERS} = require('../../config/config');
const vc = require('vc-js');
const Handlebars = require("handlebars");
const delimiters = require('handlebars-delimiters');
const hash = require('object-hash');
const {cacheInstance} = require("../utils");
const {contexts} = require('security-context');
const credentialsv1 = require('./credentialsv1.json');
delimiters(Handlebars, CUSTOM_TEMPLATE_DELIMITERS);

const KeyType = {
    RSA: "RSA", ED25519: "ED25519"
};

const getHandleBarTemplate = (credentialTemplate) => {
    const credentialTemplateHash = hash(credentialTemplate);
    if (cacheInstance.has(credentialTemplateHash)) {
        console.debug("Credential template loaded from cache");
        return cacheInstance.get(credentialTemplateHash);
    } else {
        let handleBarTemplate = Handlebars.compile(credentialTemplate);
        cacheInstance.set(credentialTemplateHash, handleBarTemplate);
        console.debug("Credential template stored in cache");
        return handleBarTemplate;
    }
};
const generateCredentials = async (data, credentialTemplate = "") => {
    console.log("Input received", credentialTemplate, data);
    const template = getHandleBarTemplate(credentialTemplate);
    let renderedTemplate = template(data);
    //TODO: find better ways to escape literals
    renderedTemplate = renderedTemplate.replaceAll("\\", "\\\\");
    const credentialData = JSON.parse(renderedTemplate);
    console.log("Sending", credentialData);
    return await signJSON(credentialData);
};


function getKeys(issuer) {
    const privateKeyPem = process.env.CERTIFICATE_PRIVATE_KEY || '-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEAnXQalrgztecTpc+INjRQ8s73FSE1kU5QSlwBdICCVJBUKiuQUt7s+Z5epgCvLVAOCbP1mm5lV7bfgV/iYWDio7lzX4MlJwDedWLiufr3Ajq+79CQiqPaIbZTo0i13zijKtX7wgxQ78wT/HkJRLkFpmGeK3za21tEfttytkhmJYlwaDTEc+Kx3RJqVhVh/dfwJGeuV4Xc/e2NH++ht0ENGuTk44KpQ+pwQVqtW7lmbDZQJoOJ7HYmmoKGJ0qt2hrj15uwcD1WEYfY5N7N0ArTzPgctExtZFDmituLGzuAZfv2AZZ9/7Y+igshzfB0reIFdUKw3cdVTzfv5FNrIqN5pwIDAQABAoIBAHPILMUoLt5UTd5f/YnebqgeCRNAmGOBcwk7HtbMqQoGF93qqvZFd30XOAJZ/ncTpz77Vl95ToxxrWk1WQLCe+ZpOK3Dgk5sFSm8zXx1T64UBNPUSnWoh37C1D39+b9rppCZScgnxlyPdSLy3h3q8Hyoy+auqUEkm/ms5W2lT3fJscyN1IAyHrhsOBWjl3Ilq5GxBo5tbYv/Fb1pQiP/p2SIHA1+2ASXNYQP100F5Vn0V6SFtBXTCQnwcvbP083NvlGxs9+xRs3MCUcxCkKepWuzYwOZDmu/2yCz1/EsP6wlsYEHmCZLdIb0tQt0caqzB/RoxfBpNRIlhOtqHvBzUgECgYEAzIRn5Y7lqO3N+V29wXXtVZjYWvBh7xUfOxAwVYv0rKI0y9kHJHhIrU+wOVOKGISxBKmzqBQRPvXtXW8E0/14Zz82g60rRwtNjvW0UoZAY3KPouwruUIjAe2UnKZcQ//MBTrvds8QGpL6nxvPsBqU0y2K+ySAOxBtNtGEjzv8nxUCgYEAxRbMWukIbgVOuQjangkfJEfA1UaRFQqQ8jUmT9aiq2nREnd4mYP8kNKzJa9L7zj6Un6yLH5DbGspZ2gGODeRw3uVFN8XSzRdLvllNEyiG/waiysUtXfG2DPOR6xD8tXXDMm/tl9gTa8cbkvqYy10XT9MpfOAsusEZVmc0/DBBMsCgYAYdAxoKjnThPuHwWma5BrIjUnxNaTADWp6iWj+EYnjylE9vmlYNvmZn1mWwSJV5Ce2QwQ0KJIXURhcf5W4MypeTfSase3mxLc1TLOO2naAbYY3GL3xnLLK3DlUsZ9+kes3BOD097UZOFG3DIA8sjDxPxTLCoY6ibBFSa/r4GRIMQKBgQCranDCgPu79RHLDVBXM0fKnj2xQXbd/hqjDmcL+Xnx7E7S6OYTXyBENX1qwVQh9ESDi34cBJVPrsSME4WVT3+PreS0CnSQDDMfr/m9ywkTnejYMdgJHOvtDuHSpJlUk3g+vxnm3H0+E5d+trhdGiOjFnLrwyWkd5OTMqWcEEFQkQKBgFfXObDz/7KqeSaAxI8RzXWbI3Fa492b4qQUhbKYVpGn98CCVEFJr11vuB/8AXYCa92OtbwgMw6Ah5JOGzRScJKdipoxo7oc2LJ9sSjjw3RB/aWl35ChvnCJhmfSL8Usbj0nWVTrPwRLjMC2bIxkLtnm9qYXPumW1EjEbusjVMpN\n-----END RSA PRIVATE KEY-----\n';
    const publicKeyPem = process.env.CERTIFICATE_PUBLIC_KEY || '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnXQalrgztecTpc+INjRQ8s73FSE1kU5QSlwBdICCVJBUKiuQUt7s+Z5epgCvLVAOCbP1mm5lV7bfgV/iYWDio7lzX4MlJwDedWLiufr3Ajq+79CQiqPaIbZTo0i13zijKtX7wgxQ78wT/HkJRLkFpmGeK3za21tEfttytkhmJYlwaDTEc+Kx3RJqVhVh/dfwJGeuV4Xc/e2NH++ht0ENGuTk44KpQ+pwQVqtW7lmbDZQJoOJ7HYmmoKGJ0qt2hrj15uwcD1WEYfY5N7N0ArTzPgctExtZFDmituLGzuAZfv2AZZ9/7Y+igshzfB0reIFdUKw3cdVTzfv5FNrIqN5pwIDAQAB\n-----END PUBLIC KEY-----\n';
    let ecRefKey = {
        privateKeyStr: "41WN3qJL5Agwg8MERbEmMLKnkNstv5iSD8oJ8sRnDyBUegeGKgjqgKm9qZTmhcLQSWCdTkSN3Cd1tPqMn1rjM3BJ",
        publicKeyStr: "DaipNW4xaH2bh1XGNNdqjnSYyru3hLnUgTBSfSvmZ2hi",
        signatureType: 'ED25519'
    };
    var issuerKeyMap = {
        'did:authorizedIssuer:23423#23': {
            privateKeyStr: privateKeyPem,
            publicKeyStr: publicKeyPem,
            signatureType: 'RSA'
        }, 'did:authorizedIssuer:23423#21': ecRefKey
    };
    if (issuerKeyMap[issuer] !== undefined) {
        let {publicKeyStr, privateKeyStr, signatureType} = issuerKeyMap[issuer];
        return {publicKey: publicKeyStr, privateKey: privateKeyStr, signatureType: signatureType};
    }
    throw new Error("Invalid issuer");
    // return {publicKey: publicKeyPem, privateKey: privateKeyPem, signatureType: KeyType.RSA};
}

function getSignatureSuite(issuer) {
    let suite = null;
    let {privateKey, publicKey, signatureType} = getKeys(issuer);
    switch (signatureType) {
        case KeyType.RSA: {
            const key = new RSAKeyPair({id: issuer, publicKeyPem: publicKey, privateKeyPem: privateKey});
            suite = new RsaSignature2018({key});
            break;
        }
        case KeyType.ED25519: {
            const key = new Ed25519KeyPair({publicKeyBase58: publicKey, privateKeyBase58: privateKey, id: issuer});
            suite = new Ed25519Signature2018({key});
        }
    }
    return suite;
}

async function signJSON(certificate) {
    let signed = "";
    const issuer = certificate?.issuer;
    let suite = getSignatureSuite(issuer);
    var controller = {
        '@context': jsigs.SECURITY_CONTEXT_URL,
        id: 'controller_id',
        publicKey: [getPublicKey(suite.type, null, issuer)], //todo add key // this authorizes this key to be used for making assertions
        assertionMethod: [issuer]
    };

    const purpose = new AssertionProofPurpose({
        controller: controller
    });
    signed = await vc.issue({
        credential: certificate, suite: suite, purpose: purpose, documentLoader: customLoader, compactProof: false
    });
    console.info("Signed cert " + JSON.stringify(signed));
    return signed;
}


const customLoader = url => {
    console.log("checking " + url);
    let documentLoaderMapping = {"https://w3id.org/security/v1": contexts.get("https://w3id.org/security/v1")};
    documentLoaderMapping['https://www.w3.org/2018/credentials#'] = credentialsv1;
    documentLoaderMapping["https://www.w3.org/2018/credentials/v1"] = credentialsv1;

    let context = documentLoaderMapping[url];
    if (context === undefined) {
        context = contexts[url];
    }
    if (context !== undefined) {
        return {
            contextUrl: null, documentUrl: url, document: context
        };
    }
    if (url.startsWith("{")) {
        return JSON.parse(url);
    }
    console.log("Fallback url lookup for document :" + url);
    return documentLoader()(url);
};

function getPublicKey(signingKeyType, publicKey = null, issuerDid = null) {
    const keyType = signingKeyType===KeyType.ED25519?'Ed25519VerificationKey2018':'RsaVerificationKey2018';
    let publicKeyObject =  {
        '@context': jsigs.SECURITY_CONTEXT_URL,
        id: issuerDid || CERTIFICATE_DID,
        type: keyType,
        controller: CERTIFICATE_CONTROLLER_ID,
    };
    let publicKeyPropertyKey = signingKeyType===KeyType.ED25519?'publicKeyBase58':'publicKeyPem';
    publicKeyObject[publicKeyPropertyKey] = publicKey;
    return publicKeyObject;
};

const verifyCredentials = async (signedCredentials, signingKeyType, externalPublicKey = null) => {
    // const publicKey = getPublicKey(signingKeyType, externalPublicKey);
    const signingType = signingKeyType === KeyType.RSA ? 'RsaVerificationKey2018' : 'Ed25519VerificationKey2018';
    // const publicKey = {
    //     '@context': jsigs.SECURITY_CONTEXT_URL,
    //     id: signedCredentials.issuer,
    //     type: signingType,
    //     controller: CERTIFICATE_CONTROLLER_ID,
    //     publicKeyPem: externalPublicKey
    // }
    let publicKeyStr = externalPublicKey;
    if (publicKeyStr === null) {
        let {publicKey} = getKeys(signedCredentials.issuer);
        publicKeyStr = publicKey;
    }

    const publicKeyObject = getPublicKey(signingKeyType, publicKeyStr, signedCredentials.issuer);
    const controller = {
        '@context': jsigs.SECURITY_CONTEXT_URL,
        id: signedCredentials.issuer,
        publicKey: [publicKeyObject], // this authorizes this key to be used for making assertions
        assertionMethod: [publicKeyObject.id]
    };
    switch (signingKeyType) {
        case KeyType.RSA:
            return await verifyRSACredentials(controller, signedCredentials, signingKeyType, publicKeyStr);
        case KeyType.ED25519:
            return await verifyED25519Credentials(controller, signedCredentials, signingKeyType, publicKeyObject);
    }
    console.log(result);
    return result;
};

const verifyRSACredentials = async (controller, signedCredentials, signingKeyType, externalPublicKey) => {
    // const key = new RSAKeyPair({...getPublicKey(signingKeyType, externalPublicKey)});
    const key = new RSAKeyPair({"id": signedCredentials.issuer, "publicKeyPem": externalPublicKey});
    const {AssertionProofPurpose} = jsigs.purposes;
    return await jsigs.verify(signedCredentials, {
        suite: new RsaSignature2018({key}),
        purpose: new AssertionProofPurpose({controller}),
        compactProof: false,
        documentLoader: customLoader
    });
};


async function verifyED25519Credentials(controller, signedCredentials, signingKeyType, publicKeyObject) {
    const key = new Ed25519KeyPair({...publicKeyObject});
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
    generateCredentials, verifyCredentials, KeyType,
};