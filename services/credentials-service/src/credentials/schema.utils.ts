import { HttpService } from '@nestjs/axios';
import { InternalServerErrorException } from '@nestjs/common';
import { AxiosResponse } from '@nestjs/terminus/dist/health-indicator/http/axios.interfaces';
import Ajv2019 from 'ajv/dist/2019';

export const getCredentialSchema = async (
  schemaId: string,
  httpService: HttpService,
) => {
  try {
    const credSchema: AxiosResponse = await httpService.axiosRef.get(
      `${process.env.SCHEMA_BASE_URL}/schema/jsonld?id=${schemaId}`,
    );
    return credSchema?.data?.schema;
  } catch (err) {
    throw new InternalServerErrorException(
      `Error fetching credential schema: ${err}`,
    );
  }
};

export const verifyCredentialSubject = (credential, schema) => {
  const ajv = new Ajv2019();
  ajv.addFormat('custom-date-time', function (dateTimeString) {
    return typeof dateTimeString === typeof new Date();
  });
  const validate = ajv.compile(schema);
  return {
    valid: validate(credential?.credentialSubject),
    errors: validate.errors,
  };
};
