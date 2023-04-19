import { SchemaService } from "src/schema/schema.service";
export declare class VerifyTemplateService {
    private schemaService;
    constructor(schemaService: SchemaService);
    private parseHBS;
    verify(template: string, schemaID: string): Promise<boolean>;
}
