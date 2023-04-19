import { Template } from '@prisma/client';
import { PrismaService } from '../prisma.service';
import { AddTemplateDTO } from './dto/addTemplate.dto';
import { ValidateTemplateService } from './validate-template.service';
type templateResponse = {
    template: string;
    schemaId: string;
    templateId: string;
    createdBy: string;
    updatedBy: string;
    createdAt: string;
    updatedAt: string;
};
export declare class RenderingTemplatesService {
    private prisma;
    private readonly verifier;
    constructor(prisma: PrismaService, verifier: ValidateTemplateService);
    getTemplateBySchemaID(schemaID: string): Promise<templateResponse[]>;
    getTemplateById(id: string): Promise<templateResponse>;
    addTemplate(addTemplateDto: AddTemplateDTO): Promise<templateResponse>;
    updateTemplate(id: string, updateTemplateDto: AddTemplateDTO): Promise<templateResponse>;
    deleteTemplate(id: string): Promise<Template>;
}
export {};
