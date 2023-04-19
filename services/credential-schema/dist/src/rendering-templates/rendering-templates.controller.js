"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.RenderingTemplatesController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const addTemplate_dto_1 = require("./dto/addTemplate.dto");
const updateTemplate_dto_1 = require("./dto/updateTemplate.dto");
const rendering_templates_service_1 = require("./rendering-templates.service");
let RenderingTemplatesController = class RenderingTemplatesController {
    constructor(renderingTemplateService) {
        this.renderingTemplateService = renderingTemplateService;
    }
    getTemplateBySchemaID(schemaId) {
        return this.renderingTemplateService.getTemplateBySchemaID(schemaId);
    }
    getTemplateById(id) {
        return this.renderingTemplateService.getTemplateById(id);
    }
    addTemplate(addTemplateDto) {
        return this.renderingTemplateService.addTemplate(addTemplateDto);
    }
    updateTemplate(id, updateTemplateDto) {
        return this.renderingTemplateService.updateTemplate(id, updateTemplateDto);
    }
    deleteTemplate(id) {
        if (this.renderingTemplateService.deleteTemplate(id)) {
            return "Credential Schema successfully deleted!";
        }
        ;
    }
};
__decorate([
    (0, common_1.Get)(),
    (0, swagger_1.ApiOperation)({ summary: "GET Templates by schema ID" }),
    (0, swagger_1.ApiOkResponse)({ status: 200 }),
    __param(0, (0, common_1.Query)('schemaId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], RenderingTemplatesController.prototype, "getTemplateBySchemaID", null);
__decorate([
    (0, common_1.Get)(':id'),
    (0, swagger_1.ApiOperation)({ summary: "GET Template by TemplateID" }),
    (0, swagger_1.ApiOkResponse)({ status: 200 }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], RenderingTemplatesController.prototype, "getTemplateById", null);
__decorate([
    (0, common_1.Post)(),
    (0, swagger_1.ApiOperation)({ summary: "Add new Template" }),
    (0, swagger_1.ApiOkResponse)({
        status: 201,
        description: "Rendering template successfully created!"
    }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [addTemplate_dto_1.AddTemplateDTO]),
    __metadata("design:returntype", void 0)
], RenderingTemplatesController.prototype, "addTemplate", null);
__decorate([
    (0, common_1.Put)(':id'),
    (0, swagger_1.ApiOperation)({ summary: "Update Template" }),
    (0, swagger_1.ApiOkResponse)({
        status: 200,
        description: "rendering template updated successfully"
    }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, updateTemplate_dto_1.UpdateTemplateDTO]),
    __metadata("design:returntype", void 0)
], RenderingTemplatesController.prototype, "updateTemplate", null);
__decorate([
    (0, common_1.Delete)(':id'),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], RenderingTemplatesController.prototype, "deleteTemplate", null);
RenderingTemplatesController = __decorate([
    (0, common_1.Controller)('template'),
    __metadata("design:paramtypes", [rendering_templates_service_1.RenderingTemplatesService])
], RenderingTemplatesController);
exports.RenderingTemplatesController = RenderingTemplatesController;
//# sourceMappingURL=rendering-templates.controller.js.map