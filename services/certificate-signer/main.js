
const {generateCredentialsRoute, verifyCredentialsRoute} = require("./src/routes/signerController");
const http = require('http');




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
    // todo: let contextsFromUrls = await getContextsFromUrls(config.CACHE_CONTEXT_URLS);
   // const customDocumentLoader = {...contextsFromUrls};
    console.log(`Server listening on port ${port}`);
});

