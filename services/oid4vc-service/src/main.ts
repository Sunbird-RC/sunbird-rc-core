import { Logger } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import {
  FastifyAdapter,
  NestFastifyApplication,
} from '@nestjs/platform-fastify';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import formbody from '@fastify/formbody';
import { AppModule } from './app.module';

async function bootstrap() {
  const adapter = new FastifyAdapter();
  // OID4VC token + direct_post endpoints use application/x-www-form-urlencoded.
  await adapter.register(formbody as any);

  const app = await NestFactory.create<NestFastifyApplication>(
    AppModule,
    adapter,
    {
      logger:
        process.env.NODE_ENV && process.env.NODE_ENV.toLowerCase() === 'debug'
          ? ['log', 'debug', 'error', 'verbose', 'warn']
          : ['error', 'warn', 'log'],
    },
  );

  const config = new DocumentBuilder()
    .setTitle('OID4VC Service')
    .setDescription('OpenID4VCI / OpenID4VP protocol facade for Sunbird RC')
    .setVersion(process.env.npm_package_version || '1.0.0')
    .addTag('OID4VCI')
    .addTag('OID4VP')
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  const port = process.env.PORT || 3400;
  await app.listen(port, '0.0.0.0');
  Logger.log(`🚀 oid4vc-service running on: http://0.0.0.0:${port}/`);
}
bootstrap();
