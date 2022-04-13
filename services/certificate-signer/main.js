const {
    CERTIFICATE_NAMESPACE
} = require("./config/config");

const config = require('./config/config');
const {vaccinationContext} = require("vaccination-context");
const {generateCredentialsRoute, verifyCredentialsRoute} = require("./src/routes/signerController");
const {setDocumentLoader} = require('certificate-signer-library/signer');
const {publicKeyPem, privateKeyPem, signingKeyType, publicKeyBase58, privateKeyBase58} = require('./config/keys');
const http = require('http');
const {getContextsFromUrls} = require("./src/utils");

console.log('Using ' + publicKeyPem);


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

const port = process.env.PORT || 4324;

const server = http.createServer(async (req, res) => {
    console.time(req.url);
    console.log(`API ${req.method} ${req.url} called`);
    try {
        if (req.method === 'POST' && req.url.startsWith("/sign")) {
            const signedData = await generateCredentialsRoute(req, res)
            res.setHeader("Content-Type", "application/json");
            res.end(JSON.stringify(signedData))

        } else if (req.method === 'POST' && req.url.startsWith("/verify")) {
            const signedData = await verifyCredentialsRoute(req, res)
            res.setHeader("Content-Type", "application/json");
            res.end(JSON.stringify(signedData))

        } else {
            res.end(`{"error": "${http.STATUS_CODES[404]}"}`)
        }
        console.timeEnd(req.url)
    } catch (e) {
        console.error(e)
        res.end(`{"error": "${http.STATUS_CODES[404]}", "message": ${e}`)
    }
});


server.listen(port, async () => {
    // add custom schema contexts
    let contextsFromUrls = await getContextsFromUrls(config.CACHE_CONTEXT_URLS);
    const customDocumentLoader = {...contextsFromUrls};
    console.log(customDocumentLoader);
    customDocumentLoader[CERTIFICATE_NAMESPACE] = vaccinationContext;
    setDocumentLoader(customDocumentLoader, signingConfig);
    console.log(`Server listening on port ${port}`);
});

