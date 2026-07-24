import { Controller, Get, Param, Header, NotFoundException } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { loadConfig } from './config/configuration';
import { SchemaClient } from './clients/schema.client';

@ApiTags('Health')
@Controller()
export class AppController {
  private readonly config = loadConfig();

  constructor(private readonly schema: SchemaClient) {}

  @ApiOperation({ summary: 'Liveness probe' })
  @Get('health')
  health() {
    return { status: 'UP', service: 'oid4vc-service' };
  }

  // Serves a schema's inline W3C VC Render Method SVG template
  // (https://www.w3.org/TR/vc-render-method/), for schemas that configure
  // `oid4vciConfig.renderMethod.svg` instead of an already-hosted `url`.
  // Referenced by `renderMethod[].id` on issued credentials — see
  // oid4vci.service.ts createOffer()/issueForSession().
  @ApiOperation({ summary: 'Inline SVG render-method template for a schema' })
  @Get('render-templates/:schemaId')
  @Header('content-type', 'image/svg+xml')
  async renderTemplate(@Param('schemaId') schemaId: string) {
    const configs = await this.schema.getOid4vciConfigs();
    const cfg = configs.find((c) => c.schemaId === schemaId);
    if (!cfg?.renderMethod?.svg) {
      throw new NotFoundException(`No inline render-method template for schema '${schemaId}'`);
    }
    return cfg.renderMethod.svg;
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
