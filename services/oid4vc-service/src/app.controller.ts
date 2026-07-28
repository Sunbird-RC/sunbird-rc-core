import { Controller, Get, Param, Header, NotFoundException } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { loadConfig } from './config/configuration';
import { SchemaClient } from './clients/schema.client';
import { Oid4vciService } from './oid4vci/oid4vci.service';

@ApiTags('Health')
@Controller()
export class AppController {
  private readonly config = loadConfig();

  constructor(
    private readonly schema: SchemaClient,
    private readonly oid4vci: Oid4vciService,
  ) {}

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

  // SD-JWT VC Type Metadata (draft-ietf-oauth-sd-jwt-vc §11), served at the
  // exact URL issuerMetadata() publishes as `vct` for schemas whose vct isn't
  // already an absolute URI (see vct.util.ts normalizeVct()). Found live:
  // walt.id's wallet resolves EVERY vct as a URL, so a bare display-name vct
  // ("National Identity Credential") crashed it on the embedded space before
  // this URI form + endpoint existed.
  //
  // Also served under the spec's `.well-known/vct` path-insertion form
  // (draft-ietf-oauth-sd-jwt-vc §6.3.1: insert `/.well-known/vct` between the
  // vct URI's authority and its path). Our vct is `<publicUrl>/vct/<slug>`,
  // so that insertion lands at `/.well-known/vct/vct/<slug>` — found live:
  // walt.id's `resolveVctUrl` fetches exactly that URL rather than the vct
  // value directly, and 404'd until this route existed too.
  @ApiOperation({ summary: 'SD-JWT VC Type Metadata for a normalized vct' })
  @Get(['vct/:slug', '.well-known/vct/vct/:slug'])
  vctMetadata(@Param('slug') slug: string) {
    return this.oid4vci.getVctTypeMetadata(slug);
  }
}
