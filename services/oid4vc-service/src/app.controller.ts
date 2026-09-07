import {
  BadRequestException,
  Controller,
  Get,
  Header,
  NotFoundException,
  Param,
  Query,
  Res,
} from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { FastifyReply } from 'fastify';
import * as QRCode from 'qrcode';
import { loadConfig } from './config/configuration';
import { KeycloakService } from './auth/keycloak.service';
import { SchemaClient } from './clients/schema.client';
import { Oid4vciService } from './oid4vci/oid4vci.service';

/**
 * Largest payload the QR endpoint will encode.
 *
 * Chosen from the format's own ceiling rather than arbitrarily: 4296 alphanumeric
 * characters is the maximum a version-40 QR symbol can carry at the lowest error
 * correction level, so anything beyond this could not be rendered anyway. The
 * largest legitimate caller is a credential-offer URI of a few hundred bytes.
 */
const MAX_QR_PAYLOAD = 4096;

@ApiTags('Health')
@Controller()
export class AppController {
  private readonly config = loadConfig();

  constructor(
    private readonly schema: SchemaClient,
    private readonly oid4vci: Oid4vciService,
    private readonly keycloak: KeycloakService,
  ) {}

  // Stays 200 even when Keycloak is down. Failing it would restart-loop the
  // container and take the wallet-facing protocol routes — which need no
  // Keycloak — down with it. The offer endpoint fails closed on its own, so a
  // Keycloak outage is reported here rather than escalated.
  @ApiOperation({ summary: 'Liveness probe' })
  @Get('health')
  async health() {
    return {
      status: 'UP',
      service: 'oid4vc-service',
      keycloak: await this.keycloak.healthInfo(),
    };
  }

  /**
   * Renders any string as a QR PNG, so consoles need no client-side QR library.
   *
   * This lives here because it previously lived in the demo app, whose source was
   * deleted while its container kept serving `/qr` from an image that can no
   * longer be rebuilt — and the verifier console renders every QR through it. The
   * `qrcode` dependency was already present, so this adds nothing to the tree.
   */
  @ApiOperation({ summary: 'Render a string as a QR code PNG' })
  @Get('qr')
  async qr(@Query('data') data: string, @Res() res: FastifyReply) {
    if (!data) throw new BadRequestException('data query parameter is required');
    // Bounded input: QR encoding is superlinear in payload size.
    if (data.length > MAX_QR_PAYLOAD) {
      throw new BadRequestException(`data exceeds ${MAX_QR_PAYLOAD} characters`);
    }
    try {
      const png = await QRCode.toBuffer(data, { width: 240, margin: 1 });
      // no-store: an offer URI is single-use, so a cached image is at best
      // useless and at worst confusing when it renders a spent offer.
      res.header('content-type', 'image/png').header('cache-control', 'no-store').send(png);
    } catch (err: any) {
      throw new BadRequestException(`Could not encode QR: ${err?.message ?? err}`);
    }
  }

  // Serves a schema's inline W3C VC Render Method SVG template
  // (https://www.w3.org/TR/vc-render-method/), for schemas that configure
  // `oid4vciConfig.renderMethod.svg` instead of an already-hosted `url`.
  // Referenced by `renderMethod[].id` on issued credentials — see
  // oid4vci.service.ts createOffer()/issueForSession().
  //
  // `renderMethod.svg` is free-form input at schema-creation time — a lower-
  // privilege action than operating this service — so serving it verbatim as
  // `image/svg+xml` would let a schema author's embedded <script> execute in
  // a browser that navigates here, on this service's own origin (stored
  // XSS). Content-Disposition: attachment forces a download instead of
  // inline rendering/execution.
  @ApiOperation({ summary: 'Inline SVG render-method template for a schema' })
  @Get('render-templates/:schemaId')
  @Header('content-type', 'image/svg+xml')
  @Header('content-disposition', 'attachment; filename="render-template.svg"')
  async renderTemplate(@Param('schemaId') schemaId: string) {
    const configs = await this.schema.getOid4vciConfigs();
    const cfg = configs.find((c) => c.schemaId === schemaId);
    if (!cfg?.renderMethod?.svg) {
      throw new NotFoundException(`No inline render-method template for schema '${schemaId}'`);
    }
    return cfg.renderMethod.svg;
  }

  // Dynamic per-type-name JSON-LD context document, referenced BY URL (not
  // inlined) from issued ldp_vc credentials' @context array. Some wallets
  // assume every @context entry deserializes as a plain string and crash on
  // an inline context object instead of a URL — so the @vocab fallback and
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
  // already an absolute URI (see vct.util.ts normalizeVct()). Some wallets
  // resolve EVERY vct as a URL, so a bare display-name vct ("National
  // Identity Credential") would crash them on the embedded space before this
  // URI form + endpoint existed.
  //
  // Also served under the spec's `.well-known/vct` path-insertion form
  // (draft-ietf-oauth-sd-jwt-vc §6.3.1: insert `/.well-known/vct` between the
  // vct URI's authority and its path). Our vct is `<publicUrl>/vct/<slug>`,
  // so that insertion lands at `/.well-known/vct/vct/<slug>` — some wallets'
  // vct resolvers fetch exactly that inserted URL rather than the vct value
  // directly, and would 404 until this route existed too.
  @ApiOperation({ summary: 'SD-JWT VC Type Metadata for a normalized vct' })
  @Get(['vct/:slug', '.well-known/vct/vct/:slug'])
  vctMetadata(@Param('slug') slug: string) {
    return this.oid4vci.getVctTypeMetadata(slug);
  }
}
