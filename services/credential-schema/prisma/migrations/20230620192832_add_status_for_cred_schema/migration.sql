-- CreateEnum
CREATE TYPE "SchemaStatus" AS ENUM ('DRAFT');

-- AlterTable
ALTER TABLE "VerifiableCredentialSchema" ADD COLUMN     "status" "SchemaStatus" NOT NULL DEFAULT 'DRAFT';
