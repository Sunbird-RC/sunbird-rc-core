import { NestFactory } from '@nestjs/core';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableCors()
  // setup swagger
  const config = new DocumentBuilder()
    .setTitle('DID Layer 3 Service')
    .setDescription('The DID Layer 3 Service API description')
    .setVersion('1.0')
    .addTag('DID Layer 3 Service')
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  await app.listen(3332);
}
bootstrap();