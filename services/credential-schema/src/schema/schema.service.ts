import {
  BadRequestException,
  Injectable,
  InternalServerErrorException,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import {
  Prisma,
  PrismaClient,
  SchemaStatus,
  VerifiableCredentialSchema,
} from '@prisma/client';
import { validate } from '../utils/schema.validator';
import { DefinedError } from 'ajv';
import { CreateCredentialDTO } from './dto/create-credentials.dto';
import { UtilsService } from '../utils/utils.service';
import { GetCredentialSchemaDTO } from './dto/getCredentialSchema.dto';

@Injectable()
export class SchemaService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly utilService: UtilsService,
  ) {}
  private logger = new Logger(SchemaService.name);
  async getCredentialSchema(
    userWhereUniqueInput: Prisma.VerifiableCredentialSchemaWhereUniqueInput, //
  ): Promise<GetCredentialSchemaDTO> {
    let schema: VerifiableCredentialSchema;
    try {
      schema = await this.prisma.verifiableCredentialSchema.findUnique({
        where: userWhereUniqueInput,
      });
    } catch (err) {
      this.logger.error('Error fetching schema from db', err);
      throw new InternalServerErrorException('Error fetching schema from db');
    }

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
      this.logger.error('schema not found for userInput', userWhereUniqueInput);
      throw new NotFoundException('Schema not found');
    }
  }

  async createCredentialSchema(
    createCredentialDto: CreateCredentialDTO,
    deprecatedId: string = undefined,
  ) {
    const data = createCredentialDto.schema;
    const tags = createCredentialDto.tags;

    // verify the Credential Schema
    if (validate(data)) {
      const didBody = {
        content: [
          {
            alsoKnownAs: [data.author, data.schema.$id],
            services: [
              {
                id: 'CredentialSchemaService',
                type: 'CredentialSchema',
              },
            ],
            method: 'schema',
          },
        ],
      };

      const did = await this.utilService.generateDID(didBody);
      this.logger.debug('DID received from identity service', did);
      const credSchema = {
        schema: {
          type: data.type,
          id: did.id,
          version: data.version,
          name: data.name,
          author: data.author,
          authored: data.authored,
          schema: data.schema,
          proof: data.proof,
        },
        tags: tags,
        status: data.status,
        deprecatedId: null,
      };
      // sign the credential schema (only the schema part of the credSchema object above)
      const proof = await this.utilService.sign(
        credSchema.schema.author,
        JSON.stringify(credSchema.schema),
      );
      credSchema.schema.proof = proof;

      try {
        const resp = await this.prisma.verifiableCredentialSchema.create({
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

        credSchema['createdAt'] = resp.createdAt;
        credSchema['updatedAt'] = resp.updatedAt;
        credSchema['deletedAt'] = resp.deletedAt;
        credSchema['createdBy'] = resp.createdBy;
        credSchema['updatedBy'] = resp.updatedBy;
      } catch (err) {
        this.logger.error('Error saving schema to db', err);
        throw new InternalServerErrorException('Error saving schema to db');
      }
      return credSchema;
    } else {
      this.logger.log('Schema validation failed', validate.errors.join('\n'));
      for (const err of validate.errors as DefinedError[]) {
        this.logger.error(err, err.message);
      }
      throw new BadRequestException(
        `Schema validation failed with the following errors: ${validate.errors.join(
          '\n',
        )}`,
      );
    }
  }

  async updateCredentialSchema(
    where: Prisma.VerifiableCredentialSchemaWhereUniqueInput,
    // data: VCSModelSchemaInterface;
    data: CreateCredentialDTO,
    //: Promise<VerifiableCredentialSchema>
  ) {
    // TODO: Deprecate the schema and create a new one
    this.logger.debug(
      'Request to update the schema where ',
      where,
      'with ',
      data,
    );

    const _where = { ...where, status: SchemaStatus.DRAFT };
    let currentSchema;
    try {
      currentSchema =
        await this.prisma.verifiableCredentialSchema.findUniqueOrThrow({
          where: _where,
        });
    } catch (err) {
      this.logger.error(err);
      throw new BadRequestException('Schema not found');
    }
    if (currentSchema.status === SchemaStatus.REVOKED) {
      this.logger.error('Scheam is already deprecated');
      throw new BadRequestException('Schema is already deprecated');
    }
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
        this.logger.error(err, err.message);
        throw new BadRequestException(err.message);
      }
    } else {
      this.logger.log(
        `Schema validation failed with ${validate.errors.join('\n')}`,
      );
      throw new BadRequestException(
        `Schema validation failed with ${validate.errors.join('\n')}}`,
      );
    }
  }

  async getSchemaByTags(tags: string[], page = 1, limit = 10) {
    this.logger.debug('Tags received to find schema', tags);

    let schemas: ReadonlyArray<VerifiableCredentialSchema>;
    try {
      schemas = await this.prisma.verifiableCredentialSchema.findMany({
        where: {
          tags: {
            hasSome: [...tags],
          },
        },
        skip: (page - 1) * limit,
        take: limit,
      });
    } catch (err) {
      this.logger.error('Error fetching credential schema from db:', err);
      throw new InternalServerErrorException('Error fetching schema from db');
    }

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
