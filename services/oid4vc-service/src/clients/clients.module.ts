import { Global, Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { CredentialsClient } from './credentials.client';
import { IdentityClient } from './identity.client';
import { SchemaClient } from './schema.client';
import { RegistryClient } from './registry.client';
import { ClaimSourceFactory } from '../claims/claim-source.factory';
import { RegistryClaimSource } from '../claims/registry.claim-source';

// HTTP clients to the existing (unchanged) Sunbird RC services, plus the claim
// sources — which live here rather than in a module of their own because this
// module is already @Global() and already owns the registry client they build on.
@Global()
@Module({
  imports: [HttpModule],
  providers: [
    CredentialsClient,
    IdentityClient,
    SchemaClient,
    RegistryClient,
    RegistryClaimSource,
    ClaimSourceFactory,
  ],
  exports: [
    CredentialsClient,
    IdentityClient,
    SchemaClient,
    RegistryClient,
    ClaimSourceFactory,
  ],
})
export class ClientsModule {}
