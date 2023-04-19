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
Object.defineProperty(exports, "__esModule", { value: true });
exports.RenderingTemplatesService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma.service");
const validate_template_service_1 = require("./validate-template.service");
let RenderingTemplatesService = class RenderingTemplatesService {
    constructor(prisma, verifier) {
        this.prisma = prisma;
        this.verifier = verifier;
    }
    async getTemplateBySchemaID(schemaID) {
        try {
            console.log(schemaID);
            const templates = await this.prisma.template.findMany({
                where: { schema: schemaID },
            });
            if (templates == null) {
                throw new common_1.NotFoundException('Template not found');
            }
            return templates.map((template) => ({
                template: template.template,
                templateId: template.id,
                schemaId: template.schema,
                createdAt: template.createdAt.toDateString(),
                createdBy: template.createdBy,
                updatedAt: template.updatedAt.toDateString(),
                updatedBy: template.updatedBy,
            }));
        }
        catch (err) {
            throw new common_1.InternalServerErrorException(err);
        }
    }
    async getTemplateById(id) {
        try {
            const template = await this.prisma.template.findFirst({
                where: {
                    id: id,
                    deleted: false,
                },
            });
            if (template == null) {
                throw new common_1.NotFoundException('Template not found');
            }
            return {
                template: template.template,
                templateId: template.id,
                schemaId: template.schema,
                createdAt: template.createdAt.toDateString(),
                createdBy: template.createdBy,
                updatedAt: template.updatedAt.toDateString(),
                updatedBy: template.updatedBy,
            };
        }
        catch (err) {
            throw new common_1.InternalServerErrorException(err);
        }
    }
    async addTemplate(addTemplateDto) {
        try {
            if (await this.verifier.verify(addTemplateDto.template, addTemplateDto.schema)) {
                const template = await this.prisma.template.create({
                    data: {
                        schema: addTemplateDto.schema,
                        template: addTemplateDto.template,
                        type: addTemplateDto.type,
                    },
                });
                return {
                    template: template.template,
                    templateId: template.id,
                    schemaId: template.schema,
                    createdAt: template.createdAt.toDateString(),
                    createdBy: template.createdBy,
                    updatedAt: template.updatedAt.toDateString(),
                    updatedBy: template.updatedBy,
                };
            }
            else {
                throw new common_1.InternalServerErrorException("Template-Schema mismatch, please check if fields in the incoming template match the fields in corresponding schema");
            }
        }
        catch (err) {
            throw new common_1.InternalServerErrorException(err);
        }
    }
    async updateTemplate(id, updateTemplateDto) {
        try {
            await this.prisma.template.updateMany({
                where: {
                    id: id,
                    deleted: false,
                },
                data: {
                    schema: updateTemplateDto.schema,
                    template: updateTemplateDto.template,
                    type: updateTemplateDto.type,
                },
            });
            const template = await this.prisma.template.findUnique({
                where: {
                    id: id,
                }
            });
            if (template == null) {
                throw new common_1.NotFoundException('Template not found');
            }
            return {
                template: template.template,
                templateId: template.id,
                schemaId: template.schema,
                createdAt: template.createdAt.toDateString(),
                createdBy: template.createdBy,
                updatedAt: template.updatedAt.toDateString(),
                updatedBy: template.updatedBy,
            };
        }
        catch (err) {
            throw new common_1.InternalServerErrorException(err);
        }
    }
    async deleteTemplate(id) {
        try {
            const templateToBeDeleted = await this.prisma.template.findUnique({
                where: {
                    id: id,
                }
            });
            if (templateToBeDeleted.deleted == true) {
                throw new common_1.NotFoundException('Record not found');
            }
            const template = await this.prisma.template.update({
                where: {
                    id: id
                },
                data: {
                    deleted: true,
                },
            });
            if (template.deleted == true) {
                return template;
            }
        }
        catch (err) {
            throw new common_1.InternalServerErrorException(err);
        }
    }
};
RenderingTemplatesService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService, validate_template_service_1.ValidateTemplateService])
], RenderingTemplatesService);
exports.RenderingTemplatesService = RenderingTemplatesService;
//# sourceMappingURL=rendering-templates.service.js.map