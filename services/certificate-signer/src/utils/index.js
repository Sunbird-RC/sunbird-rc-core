const { default: axios } = require('axios');

const getRequestBody = async (req) => {
    const buffers = []
    for await (const chunk of req) {
        buffers.push(chunk)
    }

    const data = Buffer.concat(buffers).toString();
    return JSON.parse(data);
};

const fetchTemplate = async (templateFileURL) => {
    console.log("Fetching credential templates: ", templateFileURL)
    return await axios.get(templateFileURL).then(res => res.data);
}

function isValidHttpUrl(string) {
    let url;

    try {
        url = new URL(string);
    } catch (_) {
        return false;
    }

    return url.protocol === "http:" || url.protocol === "https:";
}

module.exports = {
    getRequestBody,
    fetchTemplate,
    isValidHttpUrl
};