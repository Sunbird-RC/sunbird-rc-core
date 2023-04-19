"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const common_1 = require("@nestjs/common");
const core_1 = require("@nestjs/core");
const platform_fastify_1 = require("@nestjs/platform-fastify");
const swagger_1 = require("@nestjs/swagger");
const app_module_1 = require("./app.module");
async function bootstrap() {
    const app = await core_1.NestFactory.create(app_module_1.AppModule, new platform_fastify_1.FastifyAdapter());
    const config = new swagger_1.DocumentBuilder()
        .setTitle('Credential Schema API')
        .setDescription('APIs for creating and managing Verifiable Credential Schemas')
        .setVersion('1.0')
        .addTag('VC-Schemas')
        .build();
    const document = swagger_1.SwaggerModule.createDocument(app, config);
    swagger_1.SwaggerModule.setup('api', app, document);
    await app.listen(3000, '0.0.0.0');
    common_1.Logger.log('Listening at http://localhost:3000');
}
bootstrap();
//# sourceMappingURL=main.js.map