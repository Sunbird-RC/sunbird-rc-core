import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { Prisma, VerifiableCredentialSchema, Status as PrismaStatus } from '@prisma/client';
import { PrismaService } from '../prisma.service';
import schemas from './schemas';
import { validate } from '../utils/schema.validator';
import { DefinedError } from 'ajv';
import { VCSModelSchemaInterface } from '../types/VCModelSchema.interface';
import { VCModelSchema } from './entities/VCModelSchema.entity';
import { CreateCredentialDTO } from './dto/create-credentials.dto';
import { diff } from 'json-diff';
import { createDeflate } from 'zlib';

type schemaResponse = {
  schema: {
    // type: string,
    // id: string, 
    // version: string,
    // name: string,
    // author: string,
    // authored: string,
    // schema: {

    // }
    // proof: {

    // }
  }
  tags: string[], 
  status: PrismaStatus, 
  createdAt: string,
  createdBy: string,
  updatedAt: string,
  updatedBy: string,
}

@Injectable()
export class SchemaService {
  constructor(private readonly prisma: PrismaService) {}

  // getSchema(fileName: string): JSON {
  //   if (Object.keys(schemas).indexOf(fileName) === -1) {
  //     throw new NotFoundException(
  //       `Resource ${fileName}.json does not exist on the server`,
  //     );
  //   }

  //   return schemas[fileName];
  // }

  /*  async credentialSchemas(params: {
      skip?: number;
      take?: number;
      cursor?: Prisma.VerifiableCredentialSchemaWhereUniqueInput;
      where?: Prisma.VerifiableCredentialSchemaWhereInput;
      orderBy?: Prisma.VerifiableCredentialSchemaOrderByWithRelationInput;
    }): Promise<VerifiableCredentialSchema[]> {
      const { skip, take, cursor, where, orderBy } = params;
      return this.prisma.verifiableCredentialSchema.findMany({
        skip,
        take,
        cursor,
        where,
        orderBy,
      });
    }*/

  async credentialSchema(
    userWhereUniqueInput: Prisma.VerifiableCredentialSchemaWhereUniqueInput,
  ): Promise<schemaResponse> {

    const schema = await this.prisma.verifiableCredentialSchema.findUnique({
      where: userWhereUniqueInput,
    });

    if (schema) return {
        schema: {
          id: schema.id,
          type: schema.type,
          version: schema.version,
          name: schema.name,
          author: schema.author,
          authored: schema.authored,
          schema: schema.schema,
          proof: schema.proof,
        },
        tags: schema.tags,
        status: schema.status,
        createdBy: schema.createdBy,
        createdAt: schema.createdAt.toDateString(),
        updatedBy: schema.updatedBy,
        updatedAt: schema.updatedAt.toDateString(),
      };
    else throw new NotFoundException('Schema not found');
  }

  async createCredentialSchema(
    createCredentialDto: CreateCredentialDTO,
  ): Promise<schemaResponse> {
    // verify the Credential Schema
    const data = createCredentialDto.schema;
    const tags = createCredentialDto.tags;
    const status = createCredentialDto.status;
    if (validate(data)) {
      try {
        // generate DID using identity MS

        const createdSchema = await this.prisma.verifiableCredentialSchema.create({
          data: {
            //id: data.id,
            type: data?.type as string,
            version: data.version as string,
            name: data.name as string,
            author: data.author as string,
            authored: data.authored as string,
            schema: data.schema as Prisma.JsonValue,
            //proof will not be created here since it is not coming in the request
            proof: {},//data?.proof as Prisma.JsonValue,
            tags: tags as string[],
            // createdBy:
            // updatedBy:
            status: status,
          },
        });
        if (createdSchema) {
        return {
          schema: data,
          tags: tags as [string],
          status: status,
          createdAt: createdSchema.createdAt.toDateString(),
          updatedAt: createdSchema.updatedAt.toDateString(),
          createdBy: createdSchema.createdBy,
          updatedBy: createdSchema.updatedBy,
        }};
      } catch (err) {
        throw new BadRequestException(err.message);
      }
    } else {
      for (const err of validate.errors as DefinedError[]) {
        throw new BadRequestException(err.message);
      }
    }
  }

  async updateCredentialSchema(params: {
    where: Prisma.VerifiableCredentialSchemaWhereUniqueInput;
    data: VCSModelSchemaInterface;
  }): Promise<schemaResponse> {
    const { where, data } = params;
    const currentSchema =
      await this.prisma.verifiableCredentialSchema.findUnique({
        where,
      });
    if (currentSchema) {
      if (validate(data.schema)) {
        try {

          //if only tags and status are different a new semver will not be generated, else it will be generated.
          if (diff(currentSchema.schema,data.schema.schema)){
            //generate new semver
            const schema = await this.prisma.verifiableCredentialSchema.update({
              where,
              data: {
                // not updating ID, since ID should not be changed
                type: data?.schema.type as string,
                version: data.schema.version,
                name: data.schema.name as string,
                author: data.schema.author as string,
                authored: data.schema.authored,
                schema: data.schema.schema as Prisma.JsonValue,
                proof: {},//data?.proof as Prisma.JsonValue,
                tags: data.tags,
                status: data.status,
                // updatedBy:
                
              },
            });
            return {
              schema: data.schema,
              tags: data.tags,
              status: data.status,
              createdAt: schema.createdAt.toDateString(),
              updatedAt: schema.updatedAt.toDateString(),
              createdBy: schema.createdBy,
              updatedBy: schema.updatedBy,
            }
          }
          else{
            const schema =  await this.prisma.verifiableCredentialSchema.update({
              where,
              data: {
                // not updating ID, since ID should not be changed
                tags: data.tags,
                status: data.status,
                // updatedBy:
                
              },
            });
            return {
              schema: data.schema,
              tags: data.tags,
              status: data.status,
              createdAt: schema.createdAt.toDateString(),
              updatedAt: schema.updatedAt.toDateString(),
              createdBy: schema.createdBy,
              updatedBy: schema.updatedBy,
            }

          }

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

  async getSchemaByTags(tags: string[]): Promise<schemaResponse[]> {
    console.log('tags in service: ', tags);
    const schemaArray =  await this.prisma.verifiableCredentialSchema.findMany({
      where: {
        tags: {
          hasSome: [...tags],
        },
      },
    });
    return schemaArray.map((schema) => (
      {
        schema: {
          id: schema.id,
          type: schema.type,
          version: schema.version,
          name: schema.name,
          author: schema.author,
          authored: schema.authored,
          schema: schema.schema,
          proof: schema.proof,
        },
        tags: schema.tags,
        status: schema.status,
        createdBy: schema.createdBy,
        createdAt: schema.createdAt.toDateString(),
        updatedBy: schema.updatedBy,
        updatedAt: schema.updatedAt.toDateString(),
      }
    )
    )
  }
}
