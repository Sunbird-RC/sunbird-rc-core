const certificateController = require("./src/routes/certificate_controller");
const http = require('http');
const port = process.env.PORT || 4321;

const server = http.createServer(async (req, res) => {
    console.time(req.url)
    console.log(`API ${req.method} ${req.url} called`);
    try {
        if (req.method === 'POST' && req.url.startsWith("/api/v1/certificatePDF")) {
            const data = await certificateController.getCertificatePDF(req, res);
            res.end(data)
        } else {
            res.statusCode = 404;
            res.end("Not found");
        }
    } finally {
        if (!res.writableEnded) {
            res.statusCode = 500;
            res.end("Error occurred");
        }
    }
    console.timeEnd(req.url)
});

server.listen(port, async () => {
    console.log(`Server listening on port ${port}`);
});
