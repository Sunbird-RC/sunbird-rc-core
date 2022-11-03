const certificateController = require("./src/routes/certificate_controller");
const http = require('http');
const port = process.env.PORT || 4321;

const server = http.createServer(async (req, res) => {
    const label = `${req.url}-${new Date().getTime()}`;
    console.time(label)
    console.log(`API ${req.method} ${req.url} called`);
    try {
        if (req.method === 'GET' && req.url.startsWith("/health")) {
            res.end("OK")
        } else if (req.method === 'POST' && req.url.startsWith("/api/v1/certificate") && ["application/pdf"].includes(req.headers.accept)) {
            const data = await certificateController.getCertificatePDF(req, res);
            res.end(data)
        } else if (req.method === 'POST' && req.url.startsWith("/api/v1/certificate") && ["text/html", "image/svg+xml"].includes(req.headers.accept)) {
            const data = await certificateController.getCertificate(req, res);
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
    console.timeEnd(label)
});

server.listen(port, async () => {
    console.log(`Server listening on port ${port}`);
});
