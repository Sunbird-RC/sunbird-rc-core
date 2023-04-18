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
exports.AppController = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const swagger_1 = require("@nestjs/swagger");
const app_interface_1 = require("./app.interface");
const app_service_1 = require("./app.service");
let AppController = class AppController {
    constructor(appService, configService) {
        this.appService = appService;
        this.configService = configService;
    }
    handleHealthCheck() {
        return 'Hello World!';
    }
    issue(issueRequest) {
        return this.appService.issue(issueRequest);
    }
    async createQR(id) {
        return { image: await this.appService.renderAsQR(id) };
    }
    async renderCredential(id) {
        return { image: await this.appService.renderAsQR(id) };
    }
    verify(credential) {
        return this.appService.verify(credential);
    }
};
__decorate([
    (0, common_1.Get)(),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", String)
], AppController.prototype, "handleHealthCheck", null);
__decorate([
    (0, swagger_1.ApiOperation)({ summary: 'Sign a claim' }),
    (0, swagger_1.ApiResponse)({ type: app_interface_1.VCRequest, status: 201, description: 'Create a new VC' }),
    (0, swagger_1.ApiBody)({ type: app_interface_1.VCResponse }),
    (0, common_1.Post)('issuecred'),
    (0, common_1.HttpCode)(201),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [app_interface_1.IssueRequest]),
    __metadata("design:returntype", Object)
], AppController.prototype, "issue", null);
__decorate([
    (0, swagger_1.ApiOperation)({ summary: 'Get a Credential as a QR' }),
    (0, common_1.Get)('qr/:id'),
    (0, common_1.Render)('qrtemplate.hbs'),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], AppController.prototype, "createQR", null);
__decorate([
    (0, swagger_1.ApiOperation)({ summary: 'Render a Credential' }),
    (0, common_1.Get)('render/:id'),
    (0, common_1.Render)('credential.hbs'),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], AppController.prototype, "renderCredential", null);
__decorate([
    (0, swagger_1.ApiOperation)({ summary: 'Verify Credential' }),
    (0, swagger_1.ApiResponse)({ type: String, status: 200, description: 'Update VC' }),
    (0, common_1.Post)('verifycred'),
    (0, common_1.HttpCode)(200),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Object)
], AppController.prototype, "verify", null);
AppController = __decorate([
    (0, common_1.Controller)(),
    __metadata("design:paramtypes", [app_service_1.AppService,
        config_1.ConfigService])
], AppController);
exports.AppController = AppController;
//# sourceMappingURL=app.controller.js.map