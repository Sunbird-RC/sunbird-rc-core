import { HttpService } from '@nestjs/axios';
import { Injectable, Logger } from '@nestjs/common';
import { loadConfig } from '../config/configuration';

export interface RenderMethodConfig {
  type?: string;
  name?: string;
  url?: string;
  svg?: string;
  cssMediaQuery?: string;
}

export interface MdocConfig {
  docType: string;
  namespace: string;
  elementMapping?: Record<string, { namespace: string; elementIdentifier: string }>;
}

export interface Oid4vciSchemaConfig {
  schemaId: string;
  version: string;
  name: string;
  type: string;
  tags: string[];
  formats: string[];
  vct: string;
  display: Record<string, any>[];
  schema: any;
  // The DID that authored/owns this schema — used as the per-schema issuer
  // DID for offers created against it (see oid4vci.service.ts createOffer()).
  author: string;
  // W3C VC Render Method (https://www.w3.org/TR/vc-render-method/) config,
  // if the schema author supplied one.
  renderMethod?: RenderMethodConfig;
  // mso_mdoc (ISO/IEC 18013-5) docType/namespace config, required if
  // `formats` includes 'mso_mdoc'.
  mdoc?: MdocConfig;
}

// Reads OID4VCI-enabled schema configs from credential-schema to build the
// issuer metadata (credential_configurations_supported).
@Injectable()
export class SchemaClient {
  private readonly logger = new Logger(SchemaClient.name);
  private readonly baseUrl = loadConfig().schemaBaseUrl;

  constructor(private readonly http: HttpService) {}

  async getOid4vciConfigs(): Promise<Oid4vciSchemaConfig[]> {
    try {
      const res = await this.http.axiosRef.get(
        `${this.baseUrl}/credential-schema/oid4vci-configs`,
      );
      return res.data || [];
    } catch (err) {
      this.logger.error(`Error fetching OID4VCI schema configs: ${err}`);
      return [];
    }
  }

  async getSchema(id: string, version: string): Promise<any> {
    const res = await this.http.axiosRef.get(
      `${this.baseUrl}/credential-schema/${encodeURIComponent(id)}/${version}`,
    );
    return res.data;
  }
}
