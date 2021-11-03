const {getRequestBody} = require("../utils");
const {generateCredentials, verifyCredentials} = require("../services/signerService");

const generateCredentialsRoute = async (req, res) => {
    const reqBody = await getRequestBody(req);
    const {data, credentialTemplate} = reqBody;
    return await generateCredentials(data, credentialTemplate);
};


const verifyCredentialsRoute = async (req, res, signingKeyType) => {
    const reqBody = await getRequestBody(req);
    const {signedCredentials} = reqBody;
    return await verifyCredentials(signedCredentials, signingKeyType);
};


module.exports = {
    generateCredentialsRoute,
    verifyCredentialsRoute
};