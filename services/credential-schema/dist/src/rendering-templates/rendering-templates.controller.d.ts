import { AddTemplateDTO } from './dto/addTemplate.dto';
import { UpdateTemplateDTO } from './dto/updateTemplate.dto';
import { RenderingTemplatesService } from './rendering-templates.service';
export declare class RenderingTemplatesController {
    private readonly renderingTemplateService;
    constructor(renderingTemplateService: RenderingTemplatesService);
    getTemplateBySchemaID(schemaId: string): Promise<{
        template: string;
        schemaId: string;
        templateId: string;
        createdBy: string;
        updatedBy: string;
        createdAt: string;
        updatedAt: string;
    }[]>;
    getTemplateById(id: string): Promise<{
        template: string;
        schemaId: string;
        templateId: string;
        createdBy: string;
        updatedBy: string;
        createdAt: string;
        updatedAt: string;
    }>;
    addTemplate(addTemplateDto: AddTemplateDTO): Promise<{
        template: string;
        schemaId: string;
        templateId: string;
        createdBy: string;
        updatedBy: string;
        createdAt: string;
        updatedAt: string;
    }>;
    updateTemplate(id: string, updateTemplateDto: UpdateTemplateDTO): Promise<{
        template: string;
        schemaId: string;
        templateId: string;
        createdBy: string;
        updatedBy: string;
        createdAt: string;
        updatedAt: string;
    }>;
    deleteTemplate(id: string): string;
}
