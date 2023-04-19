import { SchemaService } from "../schema/schema.service";
export declare class ValidateTemplateService {
    private schemaService;
    constructor(schemaService: SchemaService);
    private parseHBS;
    verify(template: string, schemaID: string): Promise<boolean>;
}
