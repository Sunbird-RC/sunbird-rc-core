import { Injectable, InternalServerErrorException } from "@nestjs/common";
import { flatten } from "@nestjs/common";
import { SchemaService } from "src/schema/schema.service";


 
@Injectable()
export class ValidateTemplateService{
    constructor (private schemaService: SchemaService) {}


    private parseHBS(HBSstr: string):Array<string>{
        let HBSfields: Array<string> = HBSstr.match(/{{[{]?(.*?)[}]?}}/g)
        HBSfields.forEach((fieldname:string) => {
            let len = fieldname.length
            fieldname = fieldname.slice(2, len-2);
        })
        HBSfields.sort()
        return HBSfields;

    }

     async verify(template: string, schemaID: string): Promise<boolean> {
        try{
        let HBSfields: Array<string> = this.parseHBS(template);

        let requiredFields:Array<string> = ( await this.schemaService.credentialSchema({id:schemaID})).schema["required"];
        console.log(requiredFields);
        if (HBSfields.length == requiredFields.length){
            requiredFields.sort()
            for (let index = 0; index < HBSfields.length; index++) {
                let field = '{{'+requiredFields[index]+'}}'
                //if strings do not match:
                if (field.localeCompare(HBSfields[index])===1 || field.localeCompare(HBSfields[index])===-1){
                    return false;
                }

              } 
            return true;
        }
        else{
            console.log("Number of fields in HBS file does not match required field list in schema");
            return false;
        }} catch(err){
            throw new InternalServerErrorException(err);
        }




    }



}
