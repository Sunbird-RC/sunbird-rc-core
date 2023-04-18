"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.RenderRequestHeader = void 0;
const common_1 = require("@nestjs/common");
const renderOutput_enum_1 = require("../enums/renderOutput.enum");
function RenderRequestHeader(outputType) {
    let contentType = 'text/html; charset=utf-8';
    switch (outputType) {
        case renderOutput_enum_1.RENDER_OUTPUT.HTML:
            contentType = 'text/html; charset=utf-8';
            break;
        case renderOutput_enum_1.RENDER_OUTPUT.PDF:
            contentType = 'application/pdf';
            break;
    }
    return (0, common_1.applyDecorators)((0, common_1.Header)('Content-Type', contentType));
}
exports.RenderRequestHeader = RenderRequestHeader;
//# sourceMappingURL=render-request.decorator.js.map