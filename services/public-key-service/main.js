const http = require('http');
let configPath = (process.env.CONFIG_BASE_PATH || '.' )+ '/config.json';
console.log('loading config from ' + configPath);
let issuerKeyMap = require(configPath)
if (!issuerKeyMap) {
    console.error("Invalid config path: " + configPath)
    process.exit();
}
for (const property in issuerKeyMap.issuers) {
    delete issuerKeyMap.issuers[property].privateKey;
}
const port = process.env.PORT || 3300;

const server = http.createServer(async (req, res) => {
    console.time(req.url);
    console.log(`API ${req.method} ${req.url} called`);
    res.setHeader("Content-Type", "application/json");
    try {
        if (req.method === 'GET' && req.url.startsWith("/public-key-service/api/v1/health")) {
            res.end("OK")
        } else if (req.method === 'GET' && req.url.startsWith("/public-key-service/api/v1/public-keys")) {
            res.end(JSON.stringify(issuerKeyMap))
        } else if (req.method === 'GET' && req.url.startsWith("/public-key-service/api/v1/public-key/")) {
            let issuerId = req.url.replaceAll("/public-key-service/api/v1/public-key/", "");
            issuerId = decodeURIComponent(issuerId);
            if (issuerId in issuerKeyMap.issuers) {
                res.end(JSON.stringify(issuerKeyMap.issuers[issuerId]))
            } else {
                res.statusCode = 404;
                res.end(`{"error": "invalid issuer id"}`)
            }

        } else {
            res.statusCode = 404;
            res.end(`{"error": "${http.STATUS_CODES[404]}"}`)
        }

        console.timeEnd(req.url)
    } catch (e) {
        console.error(e)
        res.statusCode = e["code"] || 500;
        res.end(`{"error": "${http.STATUS_CODES[res.statusCode]}", "message": ${e.message}}`)
    }
});


server.listen(port, async () => {
    // add custom schema contexts
    // todo: let contextsFromUrls = await getContextsFromUrls(config.CACHE_CONTEXT_URLS);
   // const customDocumentLoader = {...contextsFromUrls};
    console.log(`Server listening on port ${port}`);
});

