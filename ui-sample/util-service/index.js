let app = require('./app')

const WFEngineFactory = require('./workflow/EngineFactory');
const baseFunctions = require('./workflow/Functions')
const engineConfig = require('./engineConfig.json')
const EPRUtilFunctions = require('./EPRFunctions')

const classesMapping = {
    'EPRFunction': EPRUtilFunctions,
    'Functions': baseFunctions
};

// Init the workflow engine with your own custom functions.
const wfEngine = WFEngineFactory.getEngine(engineConfig, classesMapping['EPRFunction'])
wfEngine.init()

app.startServer(wfEngine);