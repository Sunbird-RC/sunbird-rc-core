import { HttpService } from '@nestjs/axios';
import { Injectable, Logger } from '@nestjs/common';
import { loadConfig } from '../config/configuration';

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
