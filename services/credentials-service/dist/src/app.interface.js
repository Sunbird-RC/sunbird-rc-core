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
exports.VCUpdateRequest = exports.VCResponse = exports.IssueRequest = exports.VCRequest = void 0;
const swagger_1 = require("@nestjs/swagger");
class VCRequest {
}
__decorate([
    (0, swagger_1.ApiPropertyOptional)({
        type: String,
        description: 'Issuer of the VC',
    }),
    __metadata("design:type", String)
], VCRequest.prototype, "issuer", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Subject of the VC',
    }),
    __metadata("design:type", String)
], VCRequest.prototype, "subject", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Schema of the VC',
    }),
    __metadata("design:type", String)
], VCRequest.prototype, "schema", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Type of the VC',
    }),
    __metadata("design:type", String)
], VCRequest.prototype, "type", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Credential of the VC',
    }),
    __metadata("design:type", Object)
], VCRequest.prototype, "credential", void 0);
exports.VCRequest = VCRequest;
class IssueRequest {
}
exports.IssueRequest = IssueRequest;
class VCResponse {
}
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Context of the VC',
    }),
    __metadata("design:type", Array)
], VCResponse.prototype, "@context", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'ID of the VC',
    }),
    __metadata("design:type", String)
], VCResponse.prototype, "id", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Type of the VC',
    }),
    __metadata("design:type", Array)
], VCResponse.prototype, "type", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Issuer of the VC',
    }),
    __metadata("design:type", String)
], VCResponse.prototype, "issuer", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Date of issuance of the VC',
    }),
    __metadata("design:type", Date)
], VCResponse.prototype, "issuanceDate", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Subject of the VC',
    }),
    __metadata("design:type", Object)
], VCResponse.prototype, "credentialSubject", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({
        type: String,
        description: 'Proof of the VC',
    }),
    __metadata("design:type", Object)
], VCResponse.prototype, "proof", void 0);
exports.VCResponse = VCResponse;
class VCUpdateRequest {
}
exports.VCUpdateRequest = VCUpdateRequest;
//# sourceMappingURL=app.interface.js.map