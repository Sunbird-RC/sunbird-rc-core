import {
  CanActivate,
  ExecutionContext,
  Injectable,
  Logger,
  UnauthorizedException,
} from '@nestjs/common';
import * as jose from 'jose';
import { KeycloakService } from './keycloak.service';

/**
 * Requires a valid Keycloak realm access token, when ENABLE_AUTH=true.
 *
 * Applied per-route (see Oid4vciController.createOffer), NOT as a global
 * APP_GUARD. Almost every other route here is a public OID4VCI/OID4VP protocol
 * endpoint, and three of them (`/oid4vc/credential`, `/deferred`,
 * `/notification`) already carry a Bearer header holding this façade's OWN
 * access token — verified by TokenService against the issuer DID, not by
 * Keycloak. A global guard would reject every wallet.
 */
@Injectable()
export class KeycloakAuthGuard implements CanActivate {
  private readonly logger = new Logger(KeycloakAuthGuard.name);

  constructor(private readonly keycloak: KeycloakService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    if (!this.keycloak.enabled) return true;

    const request = context.switchToHttp().getRequest();
    const authHeader: string = request.headers?.authorization;

    if (!authHeader || !/^Bearer\s+\S/i.test(authHeader)) {
      this.deny(context, 'no bearer token');
    }

    const token = authHeader.replace(/^Bearer\s+/i, '').trim();
    try {
      // Kept on the request so a future authorization rule (roles, per-issuer
      // scoping) has the verified claims without re-parsing the token.
      request.user = await this.keycloak.verify(token);
      return true;
    } catch (err) {
      this.deny(context, this.describe(err, token));
    }
  }

  /**
   * 401 with a WWW-Authenticate challenge, per RFC 6750. The sibling guards in
   * identity-service/credential-schema `return false` instead, which NestJS
   * renders as 403 — wrong for a bearer-token API, and it tells a caller their
   * credentials were rejected rather than that they need to send some.
   */
  private deny(context: ExecutionContext, reason: string): never {
    this.logger.warn(`Rejected POST /oid4vc/offer: ${reason}`);
    context
      .switchToHttp()
      .getResponse()
      .header('WWW-Authenticate', 'Bearer error="invalid_token"');
    throw new UnauthorizedException('Invalid or missing bearer token');
  }

  /**
   * A log line an operator can act on. A wrong-realm token is the failure most
   * likely to look like a bug, so name the `iss` we got and the ones we accept
   * — the token is already untrusted, and only its issuer is logged.
   */
  private describe(err: any, token: string): string {
    if (err?.code === 'ERR_JWT_CLAIM_VALIDATION_FAILED' && err?.claim === 'iss') {
      let received = 'unreadable';
      try {
        received = String(jose.decodeJwt(token).iss);
      } catch {
        /* keep 'unreadable' */
      }
      return `token issuer '${received}' is not accepted (expected one of ${this.keycloak.issuers.join(', ')})`;
    }
    return `${err?.code || 'invalid token'}: ${err?.message || err}`;
  }
}
