const {getRequestBody, isValidHttpUrl, fetchTemplate} = require("../utils");
const {generateCredentials, verifyCredentials} = require("../services/signerService");

const generateCredentialsRoute = async (req, res) => {
    const reqBody = await getRequestBody(req);
    const {data, credentialTemplate} = reqBody;
    let template;
    if (typeof credentialTemplate === "string" && isValidHttpUrl(credentialTemplate)) {
        template = await fetchTemplate(credentialTemplate)
    } else {
        template = JSON.stringify(credentialTemplate)
    }

    return await generateCredentials(data, template);
};


const verifyCredentialsRoute = async (req, res) => {
    const reqBody = await getRequestBody(req);
    const {signedCredentials, signingKeyType, publicKey } = reqBody;
    return await verifyCredentials(signedCredentials, signingKeyType, publicKey);
};


module.exports = {
    generateCredentialsRoute,
    verifyCredentialsRoute
};