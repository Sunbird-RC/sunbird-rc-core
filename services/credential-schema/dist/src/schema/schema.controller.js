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
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.SchemaController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const create_credentials_dto_1 = require("./dto/create-credentials.dto");
const VCItem_entity_1 = require("./entities/VCItem.entity");
const VCModelSchema_entity_1 = require("./entities/VCModelSchema.entity");
const schema_service_1 = require("./schema.service");
let SchemaController = class SchemaController {
    constructor(schemaService, cacheManager) {
        this.schemaService = schemaService;
        this.cacheManager = cacheManager;
    }
    getCredentialSchema(id) {
        return this.schemaService.getCredentialSchema({ id: id });
    }
    getCredentialSchemaByTags(tags) {
        console.log(tags);
        return this.schemaService.getSchemaByTags(tags.split(','));
    }
    createCredentialSchema(body) {
        console.log(body);
        return this.schemaService.createCredentialSchema(body);
    }
    updateCredentialSchema(id, data) {
        return this.schemaService.updateCredentialSchema({
            where: { id: id },
            data,
        });
    }
};
__decorate([
    (0, common_1.Get)(':id'),
    (0, swagger_1.ApiOperation)({ summary: 'Get a Verifiable Credential Schema by id (did)' }),
    (0, swagger_1.ApiOkResponse)({
        status: 200,
        description: 'The record has been successfully created.',
        type: VCItem_entity_1.VCItem,
    }),
    (0, swagger_1.ApiNotFoundResponse)({
        status: 404,
        description: 'The record has not been found.',
    }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], SchemaController.prototype, "getCredentialSchema", null);
__decorate([
    (0, common_1.Get)(),
    (0, swagger_1.ApiQuery)({ name: 'tags', required: true, type: String }),
    (0, swagger_1.ApiOperation)({ summary: 'Get a Verifiable Credential Schema by tags' }),
    (0, swagger_1.ApiOkResponse)({
        status: 200,
        description: 'The record has been successfully obtained',
        type: VCItem_entity_1.VCItem,
    }),
    (0, swagger_1.ApiNotFoundResponse)({
        status: 404,
        description: 'The record has not been found.',
    }),
    __param(0, (0, common_1.Query)('tags')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], SchemaController.prototype, "getCredentialSchemaByTags", null);
__decorate([
    (0, common_1.Post)(),
    (0, swagger_1.ApiOperation)({ summary: 'Create a new Verifiable Credential Schema' }),
    (0, swagger_1.ApiBody)({
        type: VCModelSchema_entity_1.VCModelSchema,
    }),
    (0, swagger_1.ApiCreatedResponse)({
        status: 201,
        description: 'The record has been successfully created.',
        type: VCItem_entity_1.VCItem,
    }),
    (0, swagger_1.ApiBadRequestResponse)({
        status: 400,
        description: 'There was some problem with the request.',
    }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [create_credentials_dto_1.CreateCredentialDTO]),
    __metadata("design:returntype", Promise)
], SchemaController.prototype, "createCredentialSchema", null);
__decorate([
    (0, common_1.Put)(':id'),
    (0, swagger_1.ApiBody)({
        type: VCModelSchema_entity_1.VCModelSchema,
    }),
    (0, swagger_1.ApiOperation)({
        summary: 'Update a Verifiable Credential Schema by id (did)',
    }),
    (0, swagger_1.ApiOkResponse)({
        status: 200,
        description: 'The record has been successfully updated.',
        type: VCItem_entity_1.VCItem,
    }),
    (0, swagger_1.ApiNotFoundResponse)({
        status: 404,
        description: 'The record with the passed param id has not been found.',
    }),
    (0, swagger_1.ApiBadRequestResponse)({
        status: 400,
        description: 'There was some problem with the request.',
    }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object]),
    __metadata("design:returntype", void 0)
], SchemaController.prototype, "updateCredentialSchema", null);
SchemaController = __decorate([
    (0, common_1.Controller)('credential-schema'),
    (0, common_1.UseInterceptors)(common_1.CacheInterceptor),
    __param(1, (0, common_1.Inject)(common_1.CACHE_MANAGER)),
    __metadata("design:paramtypes", [schema_service_1.SchemaService, Object])
], SchemaController);
exports.SchemaController = SchemaController;
//# sourceMappingURL=schema.controller.js.map