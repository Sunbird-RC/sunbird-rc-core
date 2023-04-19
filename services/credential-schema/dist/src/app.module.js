"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AppModule = void 0;
const common_1 = require("@nestjs/common");
const app_controller_1 = require("./app.controller");
const app_service_1 = require("./app.service");
const schema_service_1 = require("./schema/schema.service");
const schema_module_1 = require("./schema/schema.module");
const prisma_service_1 = require("./prisma.service");
const config_1 = require("@nestjs/config");
const rendering_templates_module_1 = require("./rendering-templates/rendering-templates.module");
let AppModule = class AppModule {
};
AppModule = __decorate([
    (0, common_1.Module)({
        imports: [
            schema_module_1.SchemaModule,
            config_1.ConfigModule.forRoot({
                isGlobal: true,
            }),
            common_1.CacheModule.register({
                isGlobal: true,
                max: 1000,
            }),
            rendering_templates_module_1.RenderingTemplatesModule,
        ],
        controllers: [app_controller_1.AppController],
        providers: [app_service_1.AppService, prisma_service_1.PrismaService, schema_service_1.SchemaService],
    })
], AppModule);
exports.AppModule = AppModule;
//# sourceMappingURL=app.module.js.map