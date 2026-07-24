-- Additive columns for multi-format VC support (jwt_vc_json / vc+sd-jwt)
-- and StatusList/RevocationList2020 index allocation.
-- Defaults keep every existing row and caller behaving exactly as before.
ALTER TABLE "VerifiableCredentials" ADD COLUMN "format" TEXT NOT NULL DEFAULT 'ldp_vc';
ALTER TABLE "VerifiableCredentials" ADD COLUMN "enveloped" TEXT;
ALTER TABLE "VerifiableCredentials" ADD COLUMN "statusListIndex" INTEGER;
ALTER TABLE "VerifiableCredentials" ADD COLUMN "statusListCredential" TEXT;
