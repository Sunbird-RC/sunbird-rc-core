import axios from "axios";

const jsigs = require('jsonld-signatures');
const {RSAKeyPair, Ed25519KeyPair} = require('crypto-ld');
const {documentLoaders} = require('jsonld');
const {node: documentLoader} = documentLoaders;
const {contexts} = require('security-context');
const {CERTIFICATE_CONTROLLER_ID} = require("../config/config");
const {Ed25519Signature2018, RsaSignature2018} = jsigs.suites;
const request = require('request');
const vc = require('vc-js');
const credentialsv1 = require('./credentialsv1.json');

const KeyType = {
    RSA: "RSA", ED25519: "ED25519"
};
const DEFAULT = "default";

export const verifyCredentials = async (signedCredentials, externalPublicKey = null) => {


    // const publicKey = getPublicKey(signingKeyType, externalPublicKey);
    const signingKeyType = inferSignatureAlgoType(signedCredentials)
    // const signingKeyType = typeCode === 'RsaVerificationKey2018'?KeyType.RSA : KeyType.ED25519;
    let publicKeyStr = externalPublicKey;
    let {publicKey, verificationMethod} = await getKeys(signedCredentials.issuer);
    if (publicKeyStr === null) {
        publicKeyStr = publicKey;
    }

    const publicKeyObject = getPublicKey(signingKeyType, publicKeyStr, verificationMethod);
    const controller = {
        '@context': jsigs.SECURITY_CONTEXT_URL,
        id: verificationMethod,
        publicKey: [publicKeyObject], // this authorizes this key to be used for making assertions
        assertionMethod: [publicKeyObject.id]
    };
    switch (signingKeyType) {
        case KeyType.RSA:
            console.log("Verifying RSA");
            console.log(JSON.stringify(controller));
            return await verifyRSACredentials(controller, signedCredentials, publicKeyStr, verificationMethod);
        case KeyType.ED25519:
            return await verifyED25519Credentials(controller, signedCredentials, publicKeyObject);
    }
    return null;
};

function inferSignatureAlgoType(signedCredentials) {
    switch (signedCredentials?.proof?.type) {
        case 'RsaSignature2018':
        case 'RsaVerificationKey2018':
            return KeyType.RSA;
        default:
            return KeyType.ED25519;
    }
}


async function getKeys(issuer) { //todo move this to a config file
    const publicKeysResponse = await axios.get("/public-key-service/api/v1/public-keys")
    console.log(publicKeysResponse.data)
    let issuerKeyMap = publicKeysResponse.data;
    let key = (issuer in issuerKeyMap.issuers) ? issuer : DEFAULT;


    if (issuerKeyMap.issuers[key] !== undefined) {
        let {publicKey, privateKey, signatureType, verificationMethod} = issuerKeyMap.issuers[key];
        return {publicKey: publicKey, privateKey: privateKey, signatureType: signatureType, verificationMethod};
    }
    throw new Error("Invalid issuer");
}

function getPublicKey(signingKeyType, publicKey = null, issuerDid = null) {
    const keyType = signingKeyType === KeyType.ED25519 ? 'Ed25519VerificationKey2018' : 'RsaVerificationKey2018';
    let publicKeyObject = {
        '@context': jsigs.SECURITY_CONTEXT_URL,
        id: issuerDid,
        type: keyType,
        controller: CERTIFICATE_CONTROLLER_ID,
    };
    let publicKeyPropertyKey = signingKeyType === KeyType.ED25519 ? 'publicKeyBase58' : 'publicKeyPem';
    publicKeyObject[publicKeyPropertyKey] = publicKey;
    return publicKeyObject;
};


const verifyRSACredentials = async (controller, signedCredentials, externalPublicKey, verificationMethod) => {
    const key = new RSAKeyPair({"id": verificationMethod, "publicKeyPem": externalPublicKey});
    const {AssertionProofPurpose} = jsigs.purposes;
    return await jsigs.verify(signedCredentials, {
        suite: new RsaSignature2018({key}),
        purpose: new AssertionProofPurpose({controller}),
        compactProof: false,
        documentLoader: await customLoader
    });
};


async function verifyED25519Credentials(controller, signedCredentials, publicKeyObject) {
    const key = new Ed25519KeyPair({...publicKeyObject});
    const {AssertionProofPurpose} = jsigs.purposes;
    const purpose = new AssertionProofPurpose({
        controller: controller
    });
    return await vc.verifyCredential({
        credential: signedCredentials,
        suite: new Ed25519Signature2018({key}),
        purpose: purpose,
        documentLoader: await customLoader,
        compactProof: false
    });
}

let documentLoaderMapping = {"https://w3id.org/security/v1": contexts.get("https://w3id.org/security/v1")};
documentLoaderMapping['https://www.w3.org/2018/credentials#'] = credentialsv1;
documentLoaderMapping["https://www.w3.org/2018/credentials/v1"] = credentialsv1;


const customLoader = async url => {
    console.log("checking " + url);
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
    const urlObj = new URL(url);
    const contextResponse = await axios.get("/proxy" + urlObj.pathname, {
        headers: {
            target: urlObj.origin
        }
    });
    const loadedContext = contextResponse.data;
    console.log(loadedContext)
    documentLoaderMapping[url] = loadedContext;
    return {
        contextUrl: null, documentUrl: url, document: loadedContext
    }
};