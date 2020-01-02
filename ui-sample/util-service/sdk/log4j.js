const log4js = require('log4js');
log4js.configure({
    appenders: {
        consoleAppender: {
            type: 'console', layout: {
                type: 'pattern', pattern: '%d %[%p%] %c %f{1}:%l %m'
            }
        },
        fileAppender: {
            type: 'file', filename: 'app.log', layout: {
                type: 'pattern', pattern: '%d %p %c %f{1}:%l %m'
            }
        },
    },
    categories: {
        default: {
            appenders: ['consoleAppender', 'fileAppender'], level: 'debug', enableCallStack: true
        },
    },
});
const logger = log4js.getLogger('fileAppender');

module.exports = logger;