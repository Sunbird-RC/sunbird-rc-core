"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.validate = void 0;
const _2019_1 = require("ajv/dist/2019");
const ajv = new _2019_1.default();
const schema = require("../../schema.json");
exports.validate = ajv.compile(schema);
//# sourceMappingURL=schema.validator.js.map