import { HttpService } from '@nestjs/axios';
import {
  BadRequestException,
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';
import { AxiosResponse } from '@nestjs/terminus/dist/health-indicator/http/axios.interfaces';
import Ajv2019 from 'ajv/dist/2019';

@Injectable()
export class SchemaUtilsSerivce {
  constructor(private readonly httpService: HttpService) {}
  private logger = new Logger(SchemaUtilsSerivce.name);
  async getCredentialSchema(schemaId: string, version: string) {
    let credSchema: AxiosResponse;
    let cordSchemaId: string | null = null;
    try {
      credSchema = await this.httpService.axiosRef.get(
        `${process.env.SCHEMA_BASE_URL}/credential-schema/${schemaId}/${version}`
      );
    } catch (err) {
      throw new InternalServerErrorException(
        `Error fetching credential schema`
      );
    }
    if (credSchema.status === 404) {
      this.logger.error(
        `Schema with id ${schemaId} not found`,
        credSchema.data
      );
      throw new BadRequestException(
        `Credential schema with id ${schemaId} not found`
      );
    } else if (credSchema.status !== 200) {
      this.logger.error('Error fetching schema', credSchema.data);
      throw new InternalServerErrorException(
        `Error fetching credential schema`
      );
    }
     // If ANCHOR_TO_CORD is true, retrieve cordSchemaId
    
    if (process.env.ANCHOR_TO_CORD && process.env.ANCHOR_TO_CORD.toLowerCase().trim() === 'true') {
      this.logger.debug('Fetching cordSchemaId as ANCHOR_TO_CORD is enabled');
      cordSchemaId = credSchema?.data?.cordSchemaId || null;

      if (!cordSchemaId) {
        this.logger.warn(`Cord Schema ID not found for schema ID ${schemaId}`);
        throw new BadRequestException(`Cord Schema ID missing for schema ID ${schemaId}`);
      }
    }

    return {
      schema: credSchema?.data?.schema,  
      cordSchemaId: cordSchemaId         
    };
  }

  async getTemplateById(templateId: string) {
    try {
      const template: AxiosResponse = await this.httpService.axiosRef.get(
        `${process.env.SCHEMA_BASE_URL}/template/${templateId}`
      );
      return template?.data;
    } catch (err) {
      this.logger.error('Error fetching template', err);
      if (err.response.status === 404) {
        throw new BadRequestException(
          `Template with id ${templateId} not found`
        );
      }
      throw new InternalServerErrorException(
        `Error fetching template with id ${templateId}`
      );
    }
  }

  async verifyCredentialSubject(credential, schema) {
    const ajv = new Ajv2019({ strictTuples: false });
    ajv.addFormat('date-time', function isValidDateTime(dateTimeString) {
        // Regular expression for ISO 8601 date-time format
        const iso8601Regex = /^(\d{4}-[01]\d-[0-3]\d[T\s](?:[0-2]\d:[0-5]\d:[0-5]\d(?:\.\d+)?|23:59:60)(?:Z|[+-][0-2]\d:[0-5]\d)?)$/;

        // Check if the string matches the ISO 8601 format
        if (!iso8601Regex.test(dateTimeString)) {
          return false;
        }

        // Check if the string can be parsed into a valid date
        const date = new Date(dateTimeString);
        return !isNaN(date.getTime());
      }
    );

    const validate = ajv.compile(schema);
    return {
      valid: validate(credential?.credentialSubject),
      errors: validate.errors,
    };
  }
}
