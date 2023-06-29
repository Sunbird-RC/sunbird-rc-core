import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { flatten } from '@nestjs/common';
import { SchemaService } from '../schema/schema.service';

@Injectable()
export class ValidateTemplateService {
  constructor(private schemaService: SchemaService) {}

  private parseHBSTemplate(HBSstr: string): Array<string> {
    const hbsfields: Array<string> = HBSstr.match(/{{[{]?(.*?)[}]?}}/g);
    hbsfields.forEach((fieldname: string) => {
      const len = fieldname.length;
      fieldname = fieldname.slice(2, len - 2);
    });
    hbsfields.sort();
    return hbsfields;
  }

  async validateTemplateAgainstSchema(
    template: string,
    schemaID: string,
  ): Promise<boolean> {
    try {
      const hbsfields: Array<string> = this.parseHBSTemplate(template);

      const requiredFields: Array<string> = (
        await this.schemaService.getCredentialSchema({ id: schemaID })
      ).schema.schema['required'];
      console.log(requiredFields);
      if (hbsfields.length == requiredFields.length) {
        requiredFields.sort();
        for (let index = 0; index < hbsfields.length; index++) {
          const field = '{{' + requiredFields[index] + '}}';
          //if strings do not match:
          if (
            field.localeCompare(hbsfields[index]) === 1 ||
            field.localeCompare(hbsfields[index]) === -1
          ) {
            return false;
          }
        }
        return true;
      } else {
        console.log(
          'Number of fields in HBS file does not match required field list in schema',
        );
        return false;
      }
    } catch (err) {
      console.log('error: ', err);
      throw new InternalServerErrorException(err.message);
    }
  }
}
