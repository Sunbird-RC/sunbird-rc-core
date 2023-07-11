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

    return credSchema?.data?.schema;
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
    ajv.addFormat('custom-date-time', function (dateTimeString) {
      return typeof dateTimeString === typeof new Date();
    });
    const validate = ajv.compile(schema);
    return {
      valid: validate(credential?.credentialSubject),
      errors: validate.errors,
    };
  }
}
