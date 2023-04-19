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
exports.VCItem = void 0;
const swagger_1 = require("@nestjs/swagger");
const client_1 = require("@prisma/client");
class VCItem {
}
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'id' }),
    __metadata("design:type", String)
], VCItem.prototype, "id", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'name' }),
    __metadata("design:type", String)
], VCItem.prototype, "name", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'description' }),
    __metadata("design:type", String)
], VCItem.prototype, "description", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: Number, description: 'version of the schema' }),
    __metadata("design:type", String)
], VCItem.prototype, "version", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'type' }),
    __metadata("design:type", String)
], VCItem.prototype, "type", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'did of author' }),
    __metadata("design:type", String)
], VCItem.prototype, "author", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: Date, description: 'authored' }),
    __metadata("design:type", Date)
], VCItem.prototype, "authored", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: JSON, description: 'schema ' }),
    __metadata("design:type", Object)
], VCItem.prototype, "schema", void 0);
__decorate([
    (0, swagger_1.ApiPropertyOptional)({ type: JSON, description: 'proof' }),
    __metadata("design:type", Object)
], VCItem.prototype, "proof", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: Date, description: 'createdAt' }),
    __metadata("design:type", Date)
], VCItem.prototype, "createdAt", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: Date, description: 'updatedAt' }),
    __metadata("design:type", Date)
], VCItem.prototype, "updatedAt", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'created by' }),
    __metadata("design:type", String)
], VCItem.prototype, "createdBy", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'updated by on most recent update' }),
    __metadata("design:type", String)
], VCItem.prototype, "updatedBy", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: Date, description: 'deletedAt' }),
    __metadata("design:type", Date)
], VCItem.prototype, "deletedAt", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: [String], description: 'tags' }),
    __metadata("design:type", Array)
], VCItem.prototype, "tags", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ enum: client_1.Status, description: 'Current status of the credential schema' }),
    __metadata("design:type", String)
], VCItem.prototype, "status", void 0);
exports.VCItem = VCItem;
//# sourceMappingURL=VCItem.entity.js.map