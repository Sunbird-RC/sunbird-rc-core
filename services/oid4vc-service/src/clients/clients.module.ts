import { Global, Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { CredentialsClient } from './credentials.client';
import { IdentityClient } from './identity.client';
import { SchemaClient } from './schema.client';

// HTTP clients to the existing (unchanged) Sunbird RC services.
@Global()
@Module({
  imports: [HttpModule],
  providers: [CredentialsClient, IdentityClient, SchemaClient],
  exports: [CredentialsClient, IdentityClient, SchemaClient],
})
export class ClientsModule {}
