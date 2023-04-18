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
exports.CredentialsController = void 0;
const common_1 = require("@nestjs/common");
const credentials_service_1 = require("./credentials.service");
const getCredentialsBySubjectOrIssuer_dto_1 = require("./dto/getCredentialsBySubjectOrIssuer.dto");
const issue_credential_dto_1 = require("./dto/issue-credential.dto");
const renderTemplate_dto_1 = require("./dto/renderTemplate.dto");
const renderOutput_enum_1 = require("./enums/renderOutput.enum");
let CredentialsController = class CredentialsController {
    constructor(credentialsService) {
        this.credentialsService = credentialsService;
    }
    getCredentials(tags) {
        return this.credentialsService.getCredentials(tags.split(','));
    }
    getCredentialsBySubject(getCreds) {
        return this.credentialsService.getCredentialsBySubjectOrIssuer(getCreds);
    }
    getCredentialById(id) {
        return this.credentialsService.getCredentialById(id === null || id === void 0 ? void 0 : id.id);
    }
    issueCredentials(issueRequest) {
        return this.credentialsService.issueCredential(issueRequest);
    }
    delteCredential(id) {
        return this.credentialsService.deleteCredential(id);
    }
    verifyCredential(credId) {
        return this.credentialsService.verifyCredential(credId);
    }
    async renderTemplate(renderRequest, response) {
        let contentType = 'text/html';
        switch (renderRequest.output) {
            case renderOutput_enum_1.RENDER_OUTPUT.PDF:
                contentType = 'application/pdf';
                break;
            case renderOutput_enum_1.RENDER_OUTPUT.HTML:
                contentType = 'text/html';
                break;
        }
        response.header('Content-Type', contentType);
        return await this.credentialsService.renderCredential(renderRequest);
    }
    async getSchemaByCredId(id) {
        return this.credentialsService.getSchemaByCredId(id);
    }
};
__decorate([
    (0, common_1.Get)(),
    __param(0, (0, common_1.Query)('tags')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], CredentialsController.prototype, "getCredentials", null);
__decorate([
    (0, common_1.Post)('/search'),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [getCredentialsBySubjectOrIssuer_dto_1.GetCredentialsBySubjectOrIssuer]),
    __metadata("design:returntype", void 0)
], CredentialsController.prototype, "getCredentialsBySubject", null);
__decorate([
    (0, common_1.Get)(':id'),
    __param(0, (0, common_1.Param)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", void 0)
], CredentialsController.prototype, "getCredentialById", null);
__decorate([
    (0, common_1.Post)('issue'),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [issue_credential_dto_1.IssueCredentialDTO]),
    __metadata("design:returntype", void 0)
], CredentialsController.prototype, "issueCredentials", null);
__decorate([
    (0, common_1.Delete)(':id'),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], CredentialsController.prototype, "delteCredential", null);
__decorate([
    (0, common_1.Get)(':id/verify'),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], CredentialsController.prototype, "verifyCredential", null);
__decorate([
    (0, common_1.Post)('render'),
    __param(0, (0, common_1.Body)()),
    __param(1, (0, common_1.Res)({ passthrough: true })),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [renderTemplate_dto_1.RenderTemplateDTO, Object]),
    __metadata("design:returntype", Promise)
], CredentialsController.prototype, "renderTemplate", null);
__decorate([
    (0, common_1.Get)('schema/:id'),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], CredentialsController.prototype, "getSchemaByCredId", null);
CredentialsController = __decorate([
    (0, common_1.Controller)('credentials'),
    __metadata("design:paramtypes", [credentials_service_1.CredentialsService])
], CredentialsController);
exports.CredentialsController = CredentialsController;
//# sourceMappingURL=credentials.controller.js.map