"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.SchemaService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma.service");
const schema_validator_1 = require("../utils/schema.validator");
const json_diff_1 = require("json-diff");
let SchemaService = class SchemaService {
    constructor(prisma) {
        this.prisma = prisma;
    }
    async getCredentialSchema(userWhereUniqueInput) {
        const schema = await this.prisma.verifiableCredentialSchema.findUnique({
            where: userWhereUniqueInput,
        });
        if (schema)
            return {
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
        else
            throw new common_1.NotFoundException('Schema not found');
    }
    async createCredentialSchema(createCredentialDto) {
        const data = createCredentialDto.schema;
        const tags = createCredentialDto.tags;
        const status = createCredentialDto.status;
        if ((0, schema_validator_1.validate)(data)) {
            try {
                const createdSchema = await this.prisma.verifiableCredentialSchema.create({
                    data: {
                        type: data === null || data === void 0 ? void 0 : data.type,
                        version: data.version,
                        name: data.name,
                        author: data.author,
                        authored: data.authored,
                        schema: data.schema,
                        proof: {},
                        tags: tags,
                        status: status,
                    },
                });
                if (createdSchema) {
                    return {
                        schema: data,
                        tags: tags,
                        status: status,
                        createdAt: createdSchema.createdAt.toDateString(),
                        updatedAt: createdSchema.updatedAt.toDateString(),
                        createdBy: createdSchema.createdBy,
                        updatedBy: createdSchema.updatedBy,
                    };
                }
                ;
            }
            catch (err) {
                throw new common_1.BadRequestException(err.message);
            }
        }
        else {
            for (const err of schema_validator_1.validate.errors) {
                throw new common_1.BadRequestException(err.message);
            }
        }
    }
    async updateCredentialSchema(params) {
        const { where, data } = params;
        const currentSchema = await this.prisma.verifiableCredentialSchema.findUnique({
            where,
        });
        if (currentSchema) {
            if ((0, schema_validator_1.validate)(data.schema)) {
                try {
                    if ((0, json_diff_1.diff)(currentSchema.schema, data.schema.schema)) {
                        const schema = await this.prisma.verifiableCredentialSchema.update({
                            where,
                            data: {
                                type: data === null || data === void 0 ? void 0 : data.schema.type,
                                version: data.schema.version,
                                name: data.schema.name,
                                author: data.schema.author,
                                authored: data.schema.authored,
                                schema: data.schema.schema,
                                proof: {},
                                tags: data.tags,
                                status: data.status,
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
                        };
                    }
                    else {
                        const schema = await this.prisma.verifiableCredentialSchema.update({
                            where,
                            data: {
                                tags: data.tags,
                                status: data.status,
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
                        };
                    }
                }
                catch (err) {
                    throw new common_1.BadRequestException(err.message);
                }
            }
            else {
                for (const err of schema_validator_1.validate.errors) {
                    throw new common_1.BadRequestException(err.message);
                }
            }
        }
        else {
            throw new common_1.NotFoundException('Credential Schema not found');
        }
    }
    async getSchemaByTags(tags) {
        console.log('tags in service: ', tags);
        const schemaArray = await this.prisma.verifiableCredentialSchema.findMany({
            where: {
                tags: {
                    hasSome: [...tags],
                },
            },
        });
        return schemaArray.map((schema) => ({
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
        }));
    }
};
SchemaService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], SchemaService);
exports.SchemaService = SchemaService;
//# sourceMappingURL=schema.service.js.map