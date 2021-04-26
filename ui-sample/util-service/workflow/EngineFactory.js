var Engine = require("./Engine")
const logger = require('../sdk/log4j');

/**
 *  Creates a workflow engine.
 */

class EngineFactory {

    constructor() {
    }

    getEngine(config, functionsClass) {
        if (config.version === "1.0.0" && Array.isArray(config.rules)) {
            logger.info("Engine config 1.0.0 passed with valid rules")
            var engine = new Engine(config, functionsClass)
            return engine
        } else {
            logger.error("Engine version or rules not configured as expected")
            return null
        }
    }
}

const engineFactoryInstance = new EngineFactory();

// The one and only instance of EngineFactory.
module.exports = engineFactoryInstance;