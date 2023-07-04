import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { Prisma, SchemaStatus } from '@prisma/client';
import { PrismaService } from '../prisma.service';
import schemas from './schemas';
import { validate } from '../utils/schema.validator';
import { DefinedError } from 'ajv';
import { CreateCredentialDTO } from './dto/create-credentials.dto';
import { UtilsService } from '../utils/utils.service';
import { randomUUID } from 'crypto';

@Injectable()
export class SchemaService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly utilService: UtilsService,
  ) { }

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
      const credSchema = {
        schema: {
          type: data.type,
          id: randomUUID(),
          version: data.version,
          name: data.name,
          author: data.author,
          authored: data.authored,
          schema: data.schema,
          proof: data.proof,
        },
        tags: tags,
        status: data.status,
        createdAt: data.createdAt,
        updatedAt: data.updatedAt,
        createdBy: data.createdBy,
        updatedBy: data.updatedBy,
        deprecatedId: data.deprecatedId,
      };
      // sign the credential schema
      const proof = await this.utilService.sign(credSchema.schema.author, JSON.stringify(credSchema))
      credSchema.schema.proof = proof;
      try {
        await this.prisma.verifiableCredentialSchema.create({
          data: {
            id: credSchema.schema.id,
            type: credSchema.schema?.type as string,
            version: credSchema.schema.version,
            name: credSchema.schema.name as string,
            author: credSchema.schema.author as string,
            authored: credSchema.schema.authored,
            schema: credSchema.schema.schema as Prisma.JsonValue,
            status: credSchema.status as SchemaStatus,
            proof: credSchema.schema.proof as Prisma.JsonValue,
            tags: credSchema.tags as string[],
            deprecatedId: deprecatedId,
          },
        });
      } catch (err) {
        throw new BadRequestException(err.message);
      }
      return credSchema;
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

          const semanticVersionRegex = /^\d+(\.\d+)?$/;
          const isValidVersion = semanticVersionRegex.test(
            deprecatedSchema.version,
          );
          semanticVersionRegex.test(deprecatedSchema.version);
          if (isValidVersion) {
            const semVer = deprecatedSchema.version.split('.');
            semVer[1] = (parseInt(semVer[2]) + 1).toString();
            const newVersion = semVer.join('.');
            data.schema.version = newVersion;
          } else {
            // reset the version if the version of previous credential does not follow sementic versioning
            data.schema.version = '1.0';
          }

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
