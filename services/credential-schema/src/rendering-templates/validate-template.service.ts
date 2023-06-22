import { Injectable, InternalServerErrorException } from "@nestjs/common";
import { flatten } from "@nestjs/common";
import { SchemaService } from "src/schema/schema.service";



@Injectable()
export class ValidateTemplateService {
    constructor(private schemaService: SchemaService) {}


    private parseHBSTemplate(HBSstr: string): Array<string> {
        let hbsFields: Array<string> = HBSstr.match(/{{[{]?(.*?)[}]?}}/g)
        hbsFields.forEach((fieldname: string) => {
            let len = fieldname.length
            fieldname = fieldname.slice(2, len - 2);
        })
        hbsFields.sort()
        return hbsFields;

    }

    async validateTemplateAgainstSchema(template: string, schemaID: string): Promise<boolean> {
        try {
            let hbsFields: Array<string> = this.parseHBSTemplate(template);

            let requiredFields: Array<string> = (await this.schemaService.getCredentialSchema({ id: schemaID })).schema.schema["required"];
            console.log(requiredFields);
            if (hbsFields.length == requiredFields.length) {
                requiredFields.sort()
                for (let index = 0; index < hbsFields.length; index++) {
                    let field = '{{' + requiredFields[index] + '}}'
                    //if strings do not match:
                    if (field.localeCompare(hbsFields[index]) === 1 || field.localeCompare(hbsFields[index]) === -1) {
                        return false;
                    }

                }
                return true;
            }
            else {
                console.log("Number of fields in HBS file does not match required field list in schema");
                return false;
            }
        } catch (err) {
            throw new InternalServerErrorException(err);
        }




    }



}
