import { Controller, Get, Param, Header } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { loadConfig } from './config/configuration';

@ApiTags('Health')
@Controller()
export class AppController {
  private readonly config = loadConfig();

  @ApiOperation({ summary: 'Liveness probe' })
  @Get('health')
  health() {
    return { status: 'UP', service: 'oid4vc-service' };
  }

  // Dynamic per-type-name JSON-LD context document, referenced BY URL (not
  // inlined) from issued ldp_vc credentials' @context array. Found live: some
  // wallets (walt.id) assume every @context entry deserializes as a plain
  // string and crash ("Element class ... JsonObject is not a JsonPrimitive")
  // if handed an inline context object instead — so the @vocab fallback and
  // type-name IRI mapping (needed for ldp_vc JSON-LD safe-mode signing, see
  // oid4vci.service.ts issueForSession) live here as a real document instead.
  @ApiOperation({ summary: 'Dynamic JSON-LD context for a credential type name' })
  @Get('contexts/:typeName')
  @Header('content-type', 'application/ld+json')
  context(@Param('typeName') typeName: string) {
    return {
      '@context': {
        '@vocab': `${this.config.publicUrl}/vocab#`,
        [typeName]: `${this.config.publicUrl}/vocab#${encodeURIComponent(typeName)}`,
      },
    };
  }
}
