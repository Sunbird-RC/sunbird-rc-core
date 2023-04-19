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
exports.VCSchema = void 0;
const swagger_1 = require("@nestjs/swagger");
class VCSchema {
}
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'id' }),
    __metadata("design:type", String)
], VCSchema.prototype, "$id", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'version' }),
    __metadata("design:type", String)
], VCSchema.prototype, "$schema", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'id' }),
    __metadata("design:type", String)
], VCSchema.prototype, "description", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'name' }),
    __metadata("design:type", String)
], VCSchema.prototype, "name", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: String, description: 'author' }),
    __metadata("design:type", String)
], VCSchema.prototype, "type", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: JSON,
        description: 'properties that define a particular schema',
    }),
    __metadata("design:type", Object)
], VCSchema.prototype, "properties", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: [String], description: 'required properties' }),
    __metadata("design:type", Array)
], VCSchema.prototype, "required", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: Boolean,
        description: 'if the schema contains some additional properties',
    }),
    __metadata("design:type", Boolean)
], VCSchema.prototype, "additionalProperties", void 0);
exports.VCSchema = VCSchema;
//# sourceMappingURL=VCSchema.entity.js.map