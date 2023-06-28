import {
  BadRequestException,
  HttpException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { Prisma, SchemaStatus } from '@prisma/client';
import { PrismaService } from 'src/prisma.service';
import schemas from './schemas';
import { validate } from '../utils/schema.validator';
import { DefinedError } from 'ajv';
import { CreateCredentialDTO } from './dto/create-credentials.dto';
import { HttpService } from '@nestjs/axios';

@Injectable()
export class SchemaService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly httpService: HttpService,
  ) {}

  getSchema(fileName: string): JSON {
    if (Object.keys(schemas).indexOf(fileName) === -1) {
      throw new NotFoundException(
        `Resource ${fileName}.json does not exist on the server`,
      );
    }

    return schemas[fileName];
  }

  async getCredentialSchema(
    userWhereUniqueInput: Prisma.VerifiableCredentialSchemaWhereUniqueInput, //: Promise<VerifiableCredentialSchema>
  ) {
    console.log(userWhereUniqueInput);
    const schema = await this.prisma.verifiableCredentialSchema.findUnique({
      where: userWhereUniqueInput,
    });

    if (schema) {
      return {
        schema: {
          type: schema.type,
          id: schema.id,
          version: schema.version,
          name: schema.name,
          author: schema.author,
          authored: schema.authored,
          schema: schema.schema,
          proof: schema.proof,
        },
        tags: schema.tags,
        status: schema.status,
        createdAt: schema.createdAt,
        updatedAt: schema.updatedAt,
        createdBy: schema.createdBy,
        updatedBy: schema.updatedBy,
        deprecatedId: schema.deprecatedId,
      };
    } else {
      throw new NotFoundException('Schema not found');
    }
  }

  async createCredentialSchema(
    createCredentialDto: CreateCredentialDTO,
    deprecatedId: string = undefined,
  ) {
    // verify the Credential Schema
    const data = createCredentialDto.schema;
    const tags = createCredentialDto.tags;
    if (validate(data)) {
      let VCSchema;
      try {
        const schema = await this.prisma.verifiableCredentialSchema.create({
          data: {
            // id: data.id,
            type: data?.type as string,
            version: data.version,
            name: data.name as string,
            author: data.author as string,
            authored: data.authored,
            schema: data.schema as Prisma.JsonValue,
            status: data.status as SchemaStatus,
            // proof: {}, // data?.proof as Prisma.JsonValue,
            tags: tags as string[],
            deprecatedId: deprecatedId,
          },
        });
        VCSchema = {
          schema: {
            type: schema.type,
            id: schema.id,
            version: schema.version,
            name: schema.name,
            author: schema.author,
            authored: schema.authored,
            schema: schema.schema,
            proof: schema.proof,
          },
          tags: schema.tags,
          status: schema.status,
          createdAt: schema.createdAt,
          updatedAt: schema.updatedAt,
          createdBy: schema.createdBy,
          updatedBy: schema.updatedBy,
          deprecatedId: schema.deprecatedId,
        };
      } catch (err) {
        throw new BadRequestException(err.message);
      }
      try {
        const signedVCResponse = await this.httpService.axiosRef.post(
          `${process.env.IDENTITY_BASE_URL}/utils/sign`,
          {
            DID: VCSchema.schema.author,
            payload: JSON.stringify(VCSchema),
          },
        );

        VCSchema.schema.proof = {
          proofValue: signedVCResponse.data.signed as string,
          proofPurpose: 'assertionMethod',
          created: new Date().toISOString(),
          type: 'Ed25519Signature2020',
          verificationMethod: VCSchema.schema.author,
        };

        // store proof in database
        await this.prisma.verifiableCredentialSchema.update({
          where: { id: VCSchema.schema.id },
          data: {
            proof: VCSchema.schema.proof,
          },
        });

        return VCSchema;
      } catch (err) {
        console.error(err.response.data);
        throw new HttpException("Couldn't sign the schema", 500);
      }
    } else {
      for (const err of validate.errors as DefinedError[]) {
        throw new BadRequestException(err.message);
      }
    }
  }

  async updateCredentialSchema(
    params: {
      where: Prisma.VerifiableCredentialSchemaWhereUniqueInput;
      // data: VCSModelSchemaInterface;
      data: CreateCredentialDTO;
    }, //: Promise<VerifiableCredentialSchema>
  ) {
    // TODO: Deprecate the schema and create a new one
    const { where, data } = params;
    console.log('where: ', where);
    console.log('data: ', data);
    const currentSchema =
      await this.prisma.verifiableCredentialSchema.findUnique({
        where,
      });
    if (currentSchema.status === SchemaStatus.REVOKED)
      throw new BadRequestException('Schema is already deprecated');
    if (currentSchema) {
      if (validate(data.schema)) {
        try {
          // deprecate the current schema
          const deprecatedSchema =
            await this.prisma.verifiableCredentialSchema.update({
              where,
              data: {
                status: SchemaStatus.REVOKED,
              },
            });

          // create a new schema
          // const semanticVersionRegex = /^\d+(\.\d+)?$/;
          // const isValidVersion = semanticVersionRegex.test(
          //   deprecatedSchema.version,
          // );
          // semanticVersionRegex.test(deprecatedSchema.version);
          // if (isValidVersion) {
          //   const semVer = deprecatedSchema.version.split('.');
          //   semVer[1] = (parseInt(semVer[2]) + 1).toString();
          //   const newVersion = semVer.join('.');
          //   data.schema.version = newVersion;
          // } else {
          //   // reset the version if the version of previous credential does not follow sementic versioning
          //   data.schema.version = '1.0';
          // }
          return await this.createCredentialSchema(data, deprecatedSchema?.id);
        } catch (err) {
          throw new BadRequestException(err.message);
        }
      } else {
        for (const err of validate.errors as DefinedError[]) {
          throw new BadRequestException(err.message);
        }
      }
    } else {
      throw new NotFoundException('Credential Schema not found');
    }
  }

  async getSchemaByTags(tags: string[], page = 1, limit = 10) {
    console.log('tags in service: ', tags);
    const schemas = await this.prisma.verifiableCredentialSchema.findMany({
      where: {
        tags: {
          hasSome: [...tags],
        },
      },
      skip: (page - 1) * limit,
      take: limit,
    });

    return schemas.map((schema) => {
      return {
        schema: {
          type: schema.type,
          id: schema.id,
          version: schema.version,
          name: schema.name,
          author: schema.author,
          authored: schema.authored,
          schema: schema.schema,
          proof: schema.proof,
        },
        tags: schema.tags,
        status: schema.status,
        createdAt: schema.createdAt,
        updatedAt: schema.updatedAt,
        createdBy: schema.createdBy,
        updatedBy: schema.updatedBy,
        deprecatedId: schema.deprecatedId,
      };
    });
  }
}
