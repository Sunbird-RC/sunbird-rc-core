import {
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';
import { SchemaService } from '../schema/schema.service';

@Injectable()
export class ValidateTemplateService {
  constructor(private schemaService: SchemaService) {}
  private logger = new Logger(ValidateTemplateService.name);
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
      this.logger.debug('Required fields in Template', requiredFields);

      if (hbsfields.length == requiredFields.length) {
        requiredFields.sort();
        for (let index = 0; index < hbsfields.length; index++) {
          const field = '{{' + requiredFields[index] + '}}';
          //if strings do not match:
          if (
            field.localeCompare(hbsfields[index]) === 1 ||
            field.localeCompare(hbsfields[index]) === -1
          ) {
            this.logger.log(
              'Template not validated against schema successfully, strings do not match',
            );
            return false;
          }
        }
        this.logger.log('Template validated successfully');
        return true;
      } else {
        this.logger.log(
          'Number of fields in HBS file does not match required field list in schema',
        );
        return false;
      }
    } catch (err) {
      this.logger.error(err);
      throw new InternalServerErrorException(err.message);
    }
  }
}
