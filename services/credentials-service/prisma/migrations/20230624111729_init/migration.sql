-- CreateEnum
CREATE TYPE "VCStatus" AS ENUM ('PENDING', 'ISSUED', 'REVOKED');

-- CreateTable
CREATE TABLE "VerifiableCredentials" (
    "id" TEXT NOT NULL,
    "type" TEXT[],
    "issuer" TEXT NOT NULL,
    "issuanceDate" TEXT NOT NULL,
    "expirationDate" TEXT NOT NULL,
    "credential_schema" TEXT NOT NULL,
    "subject" JSONB NOT NULL,
    "subjectId" TEXT NOT NULL,
    "unsigned" JSONB,
    "signed" JSONB,
    "proof" JSONB,
    "status" "VCStatus" NOT NULL DEFAULT 'ISSUED',
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    "createdBy" TEXT,
    "updatedBy" TEXT,
    "tags" TEXT[],

    CONSTRAINT "VerifiableCredentials_pkey" PRIMARY KEY ("id")
);
