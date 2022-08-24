const express = require('express');
const morgan = require("morgan");
const { createProxyMiddleware } = require('http-proxy-middleware');

// Create Express Server
const app = express();

// Configuration
const PORT = process.env.PORT || 4400;
const HOST = "0.0.0.0";

app.use(morgan('dev'));

// Info GET endpoint
app.get('/info', (req, res, next) => {
    res.send('This is a custom proxy service.');
});

// health GET endpoint
app.get('/health', (req, res, next) => {
    res.send('{"status": "up"}');
});

// Proxy endpoints
app.use('/proxy', createProxyMiddleware({
    changeOrigin: true,
    pathRewrite: {
        [`^/proxy`]: '',
    },
    router: function(req) {
        return req.header("target");
    }
}));

// Start the Proxy
app.listen(PORT, HOST, () => {
    console.log(`Starting Proxy at ${HOST}:${PORT}`);
});

