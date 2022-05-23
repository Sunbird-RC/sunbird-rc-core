const {getRequestBody, isValidHttpUrl, fetchTemplate} = require("../utils");
const {generateCredentials, verifyCredentials} = require("../services/signerService");
const {signingKeyType} = require('../../config/keys');

const generateCredentialsRoute = async (req) => {
    const reqBody = await getRequestBody(req);
    const {data, credentialTemplate} = reqBody;
    let template = credentialTemplate;
    if (typeof template === "string" && isValidHttpUrl(template)) {
        template = await fetchTemplate(template)
    }

    if (typeof template !== "string") {
        template = JSON.stringify(template)
    }

    return await generateCredentials(data, template);
};


const verifyCredentialsRoute = async (req) => {
    const reqBody = await getRequestBody(req);
    const {signedCredentials, publicKey } = reqBody;

    return await verifyCredentials(signedCredentials, signingKeyType, publicKey);
};


module.exports = {
    generateCredentialsRoute,
    verifyCredentialsRoute
};