import { Global, Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { KeycloakAuthGuard } from './auth.guard';
import { KeycloakService } from './keycloak.service';

// Global so a controller can `@UseGuards(KeycloakAuthGuard)` without its module
// importing anything — same arrangement as ClientsModule.
@Global()
@Module({
  imports: [HttpModule],
  providers: [KeycloakService, KeycloakAuthGuard],
  exports: [KeycloakService, KeycloakAuthGuard],
})
export class AuthModule {}
