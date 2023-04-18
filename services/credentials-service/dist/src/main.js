"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const common_1 = require("@nestjs/common");
const core_1 = require("@nestjs/core");
const platform_fastify_1 = require("@nestjs/platform-fastify");
const swagger_1 = require("@nestjs/swagger");
const path_1 = require("path");
const app_module_1 = require("./app.module");
async function bootstrap() {
    const app = await core_1.NestFactory.create(app_module_1.AppModule, new platform_fastify_1.FastifyAdapter());
    app.setViewEngine({
        engine: {
            handlebars: require('handlebars'),
        },
        templates: (0, path_1.join)(__dirname, '..', 'views'),
    });
    app.enableCors();
    const config = new swagger_1.DocumentBuilder()
        .setTitle('Verifiable Credentials Manager')
        .setDescription('Issue and Verify Verifiable Credentials')
        .setVersion('1.0')
        .addTag('vc')
        .build();
    const document = swagger_1.SwaggerModule.createDocument(app, config);
    swagger_1.SwaggerModule.setup('api', app, document);
    const port = process.env.PORT || 3333;
    await app.startAllMicroservices();
    await app.listen(port, '0.0.0.0');
    common_1.Logger.log(`🚀 Application is running on: http://localhost:${port}/`);
}
bootstrap();
//# sourceMappingURL=main.js.map