/*
  Warnings:

  - You are about to drop the `Counter` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `Presentations` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `VC` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `VCV2` table. If the table is not empty, all the data it contains will be lost.

*/
-- DropForeignKey
ALTER TABLE "VCV2" DROP CONSTRAINT "VCV2_presentationsId_fkey";

-- DropTable
DROP TABLE "Counter";

-- DropTable
DROP TABLE "Presentations";

-- DropTable
DROP TABLE "VC";

-- DropTable
DROP TABLE "VCV2";

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
    "presentationsId" TEXT,

    CONSTRAINT "VerifiableCredentials_pkey" PRIMARY KEY ("id")
);
