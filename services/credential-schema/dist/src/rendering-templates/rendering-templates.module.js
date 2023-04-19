"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.RenderingTemplatesModule = void 0;
const common_1 = require("@nestjs/common");
const rendering_templates_service_1 = require("./rendering-templates.service");
const rendering_templates_controller_1 = require("./rendering-templates.controller");
const prisma_service_1 = require("../prisma.service");
const validate_template_service_1 = require("./validate-template.service");
const schema_module_1 = require("../schema/schema.module");
const schema_service_1 = require("../schema/schema.service");
let RenderingTemplatesModule = class RenderingTemplatesModule {
};
RenderingTemplatesModule = __decorate([
    (0, common_1.Module)({
        imports: [schema_module_1.SchemaModule],
        providers: [rendering_templates_service_1.RenderingTemplatesService, prisma_service_1.PrismaService, validate_template_service_1.ValidateTemplateService, schema_service_1.SchemaService],
        controllers: [rendering_templates_controller_1.RenderingTemplatesController],
    })
], RenderingTemplatesModule);
exports.RenderingTemplatesModule = RenderingTemplatesModule;
//# sourceMappingURL=rendering-templates.module.js.map