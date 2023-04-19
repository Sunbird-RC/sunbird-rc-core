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
exports.VCModelSchema = void 0;
const swagger_1 = require("@nestjs/swagger");
const client_1 = require("@prisma/client");
class VCModelSchema {
}
__decorate([
    (0, swagger_1.ApiProperty)({ type: Object, description: 'Schema payload' }),
    __metadata("design:type", Object)
], VCModelSchema.prototype, "schema", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ type: (Array), description: 'tags for the schema' }),
    __metadata("design:type", Array)
], VCModelSchema.prototype, "tags", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ enum: client_1.Status, description: 'current schema staus' }),
    __metadata("design:type", String)
], VCModelSchema.prototype, "status", void 0);
exports.VCModelSchema = VCModelSchema;
//# sourceMappingURL=VCModelSchema.entity.js.map