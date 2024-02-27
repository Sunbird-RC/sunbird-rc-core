import { NestFactory } from '@nestjs/core';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { Logger } from '@nestjs/common';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableCors()
  // setup swagger
  const config = new DocumentBuilder()
    .setTitle('Identity Service')
    .setDescription('The Identity Service API description')
    .setVersion(process.env.npm_package_version)
    .addTag('Identity Service')
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);
  const port = process.env.PORT || 3332;
  await app.listen(port, '0.0.0.0');
  Logger.log(`🚀 Application is running on: http:// .0.0.0:${port}/`);
}
bootstrap();