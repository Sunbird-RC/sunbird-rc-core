import { VCModelSchema } from '../entities/VCModelSchema.entity';

// W3C VC Render Method (https://www.w3.org/TR/vc-render-method/) config for
// this schema. Provide either `url` (an already-hosted template, used as-is)
// or `svg` (inline markup, hosted by oid4vc-service at
// /render-templates/:schemaId with a computed digestMultibase) — not both.
export interface RenderMethodConfig {
  type?: string; // default 'SvgRenderingTemplate'
  name?: string;
  url?: string;
  svg?: string;
  cssMediaQuery?: string;
}

// mso_mdoc (ISO/IEC 18013-5) claims are organized under
// {namespace: {elementIdentifier: value}}, not a flat credentialSubject.
// `namespace` is the single default namespace new claims are placed under;
// `elementMapping` remaps a specific claim name to a different
// {namespace, elementIdentifier} pair when it doesn't already match the
// default namespace/an ISO-registered element identifier.
export interface MdocConfig {
  docType: string;
  namespace: string;
  elementMapping?: Record<string, { namespace: string; elementIdentifier: string }>;
}

export interface Oid4vciConfig {
  oid4vciEnabled: boolean;
  oid4vciFormats?: string[]; // e.g. ['ldp_vc', 'jwt_vc_json', 'vc+sd-jwt', 'mso_mdoc']
  vct?: string;
  display?: Record<string, any>[];
  renderMethod?: RenderMethodConfig;
  mdoc?: MdocConfig; // required if oid4vciFormats includes 'mso_mdoc'
}

export class CreateCredentialDTO {
  schema: VCModelSchema;
  tags: string[];
  status: any;
  deprecatedId?: string;
  oid4vciConfig?: Oid4vciConfig; // optional OID4VCI opt-in
}
