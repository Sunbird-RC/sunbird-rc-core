import { Logger } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import {
  FastifyAdapter,
  NestFastifyApplication,
} from '@nestjs/platform-fastify';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create<NestFastifyApplication>(
    AppModule,
    new FastifyAdapter(),
    {
      logger:
        process.env.NODE_ENV && process.env.NODE_ENV.toLowerCase() === 'debug'
          ? ['log', 'debug', 'error', 'verbose', 'warn']
          : ['error', 'warn', 'log'],
    },
  );

  const config = new DocumentBuilder()
    .setTitle('Credential Schema API')
    .setDescription(
      'APIs for creating and managing Verifiable Credential Schemas',
    )
    .setVersion(process.env.npm_package_version)
    .addTag('VC-Schemas')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  const port = process.env.PORT || 3333;
  await app.listen(port, '0.0.0.0');
  Logger.log(`🚀 Application is running on: http://0.0.0.0:${port}/`);
}
bootstrap();
