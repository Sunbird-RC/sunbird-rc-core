const {getRequestBody} = require("../utils");
const {generateCredentials, verifyCredentials} = require("../services/signerService");

const generateCredentialsRoute = async (req, res) => {
    const reqBody = await getRequestBody(req);
    const {data, credentialTemplate} = reqBody;
    return await generateCredentials(data, credentialTemplate);
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