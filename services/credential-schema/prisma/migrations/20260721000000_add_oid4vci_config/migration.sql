-- Additive nullable column for per-schema OID4VCI opt-in config.
-- Absent/null = current behaviour (schema not wallet-enabled).
ALTER TABLE "VerifiableCredentialSchema" ADD COLUMN "oid4vciConfig" JSONB;
