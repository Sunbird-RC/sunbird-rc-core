const {default: axios} = require('axios');
const NodeCache = require("node-cache");

const getRequestBody = async (req) => {
    const buffers = []
    for await (const chunk of req) {
        buffers.push(chunk)
    }

    const data = Buffer.concat(buffers).toString();
    return JSON.parse(data);
};

const fetchTemplate = async (templateFileURL) => {
    console.log("Fetching credential templates: ", templateFileURL);
    const template = cacheInstance.get(templateFileURL);
    if (template === undefined) {
        let template = await axios.get(templateFileURL).then(res => res.data);
        cacheInstance.set(templateFileURL, template);
        console.debug("Fetched credential templates from API");
        return template;
    } else {
        console.debug("Fetched credential templates from cache");
        return template;
    }
};

function isValidHttpUrl(string) {
    let url;

    try {
        url = new URL(string);
    } catch (_) {
        return false;
    }

    return url.protocol === "http:" || url.protocol === "https:";
}

const cacheInstance = new NodeCache();

const getContextsFromUrls = async (urls) => {
    const contexts = {};
    if (urls && urls.length > 0) {
        for (const url of urls.split(",")) {
            contexts[url] = await fetchTemplate(url);
        }
    }
    return contexts;
};

module.exports = {
    getRequestBody,
    fetchTemplate,
    isValidHttpUrl,
    cacheInstance,
    getContextsFromUrls
};