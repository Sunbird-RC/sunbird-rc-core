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
    .setVersion('1.0')
    .addTag('VC-Schemas')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  await app.listen(3000, '0.0.0.0');
  Logger.log('Listening at http://localhost:3000');
}
bootstrap();
