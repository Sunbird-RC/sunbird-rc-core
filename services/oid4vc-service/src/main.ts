import { Logger } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import {
  FastifyAdapter,
  NestFastifyApplication,
} from '@nestjs/platform-fastify';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { KeycloakService } from './auth/keycloak.service';

async function bootstrap() {
  // OID4VC token + direct_post endpoints use application/x-www-form-urlencoded;
  // FastifyAdapter registers this content-type parser by default.
  const adapter = new FastifyAdapter();

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

  // Allow browser-based clients (e.g. the test-wallet web UI) to call the
  // wallet-facing endpoints cross-origin. Safe for these public protocol
  // routes (offer/token/nonce/credential, vp/*, .well-known/*), which carry no
  // ambient cookie/session auth — the only credential is a Bearer access token.
  app.enableCors();

  // With ENABLE_AUTH=true this exits the process if Keycloak never answers:
  // there is no point serving an offer endpoint we cannot authorise. No-op
  // when the flag is off.
  await app.get(KeycloakService).probeOrExit();

  const config = new DocumentBuilder()
    .setTitle('OID4VC Service')
    .setDescription('OpenID4VCI / OpenID4VP protocol facade for Sunbird RC')
    .setVersion(process.env.npm_package_version || '1.0.0')
    .addTag('OID4VCI')
    .addTag('OID4VP')
    // Only POST /oid4vc/offer uses this, and only when ENABLE_AUTH=true.
    .addBearerAuth()
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  const port = process.env.PORT || 3400;
  await app.listen(port, '0.0.0.0');
  Logger.log(`🚀 oid4vc-service running on: http://0.0.0.0:${port}/`);
}
// A failed boot must be a clean non-zero exit, not an unhandled rejection —
// the auth config is validated during startup and the container needs to see
// the failure.
bootstrap().catch((err) => {
  Logger.error(`oid4vc-service failed to start: ${err?.message || err}`);
  process.exit(1);
});
