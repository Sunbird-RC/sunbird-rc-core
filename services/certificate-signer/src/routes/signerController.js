const {getRequestBody, isValidHttpUrl, fetchTemplate} = require("../utils");
const {generateCredentials, verifyCredentials} = require("../services/signerService");

const generateCredentialsRoute = async (req) => {
    const reqBody = await getRequestBody(req);

    function isValidRequestBody(reqBody) {
        if ("data" in reqBody && "credentialTemplate" in reqBody) {
            return true;
        }
        return false;
    }

    if (!isValidRequestBody(reqBody)) {
        throw {"code":400, "message":"Bad request"}
    }
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


const verifyCredentialsRoute = async (req, res) => {
    const reqBody = await getRequestBody(req);
    const {signedCredentials, publicKey } = reqBody;

    if (isValidSignedCredentials(signedCredentials)) {
        return await verifyCredentials(signedCredentials, publicKey);
    } else {
        throw {"code":400, "message":"Bad request"}
    }
};

function isValidSignedCredentials(signedCredentials) {
    if (typeof(signedCredentials) !== "object") {
        return false;
    }
    if (!("issuer" in signedCredentials) ||
        !("proof" in signedCredentials))
        return false;

    return true;
}


module.exports = {
    generateCredentialsRoute,
    verifyCredentialsRoute
};