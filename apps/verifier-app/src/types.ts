/** A credential type the user can ask for, derived from a live schema config. */
export type CredentialType = {
  /** schemaId, e.g. did:schema:c869c569-… */
  id: string
  name: string
  /** OID4VCI credential_configuration_id for the vc+sd-jwt variant. */
  configId: string
  /** Absolute vct URI, as embedded in issued credentials and matched by DCQL. */
  vct: string
  /** The DID that signs this type's credentials. */
  issuer: string
  /** Every disclosable attribute, from the schema's `properties`. */
  attributes: string[]
  /** Per-attribute descriptions from the schema, for tooltips/labels. */
  descriptions: Record<string, string>
  /** JSON types, used to synthesise plausible sample values. */
  jsonTypes: Record<string, string>
}
