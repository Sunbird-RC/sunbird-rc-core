const NodePolyfillPlugin = require("node-polyfill-webpack-plugin")

module.exports = {
    // Other rules...
    resolve: {
        fallback: {
            "fs": false,
            "tls": false,
            "net": false,
            "path": require.resolve("path-browserify"),
            "zlib": false,
            "http": false,
            "https": require.resolve("https-browserify"),
            "stream": false,
            "crypto": false,
            "crypto-browserify": require.resolve('crypto-browserify'), //if you want to use this module also don't forget npm i crypto-browserify
        }
    },
}