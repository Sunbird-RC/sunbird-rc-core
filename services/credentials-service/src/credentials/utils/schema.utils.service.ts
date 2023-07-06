import { HttpService } from "@nestjs/axios";
import { Injectable, InternalServerErrorException } from "@nestjs/common";
import { AxiosResponse } from "@nestjs/terminus/dist/health-indicator/http/axios.interfaces";
import Ajv2019 from "ajv/dist/2019";

@Injectable()
export class SchemaUtilsSerivce {
  constructor(private readonly httpService: HttpService) {}

  async getCredentialSchema(schemaId: string) {
    try {
      const credSchema: AxiosResponse = await this.httpService.axiosRef.get(
        `${process.env.SCHEMA_BASE_URL}/${schemaId}`
      );
      return credSchema?.data?.schema;
    } catch (err) {
      throw new InternalServerErrorException(
        `Error fetching credential schema`
      );
    }
  }

  async getTemplateById(templateId: string) {
    try {
      const template: AxiosResponse = await this.httpService.axiosRef.get(
        `${process.env.SCHEMA_BASE_URL}/rendering-template/${templateId}`
      );
      return template?.data;
    } catch (err) {
      throw new InternalServerErrorException(
        `Error fetching template with id ${templateId}`
      );
    }
  }

  async verifyCredentialSubject(credential, schema) {
    const ajv = new Ajv2019();
    ajv.addFormat("custom-date-time", function (dateTimeString) {
      return typeof dateTimeString === typeof new Date();
    });
    const validate = ajv.compile(schema);
    return {
      valid: validate(credential?.credentialSubject),
      errors: validate.errors,
    };
  }
}
