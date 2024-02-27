/*
  Warnings:

  - The values [ACTIVE,DEPRECATED] on the enum `SchemaStatus` will be removed. If these variants are still used in the database, this will fail.

*/
-- AlterEnum
BEGIN;
CREATE TYPE "SchemaStatus_new" AS ENUM ('DRAFT', 'PUBLISHED', 'REVOKED');
ALTER TABLE "VerifiableCredentialSchema" ALTER COLUMN "status" DROP DEFAULT;
ALTER TABLE "VerifiableCredentialSchema" ALTER COLUMN "status" TYPE "SchemaStatus_new" USING ("status"::text::"SchemaStatus_new");
ALTER TYPE "SchemaStatus" RENAME TO "SchemaStatus_old";
ALTER TYPE "SchemaStatus_new" RENAME TO "SchemaStatus";
DROP TYPE "SchemaStatus_old";
ALTER TABLE "VerifiableCredentialSchema" ALTER COLUMN "status" SET DEFAULT 'DRAFT';
COMMIT;
