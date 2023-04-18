-- CreateEnum
CREATE TYPE "VCStatus" AS ENUM ('PENDING', 'ISSUED', 'REVOKED');

-- CreateTable
CREATE TABLE "VC" (
    "id" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "subject" TEXT NOT NULL,
    "issuer" TEXT NOT NULL,
    "status" "VCStatus" NOT NULL DEFAULT 'PENDING',
    "credential_schema" TEXT NOT NULL,
    "unsigned" JSONB NOT NULL,
    "signed" JSONB,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "VC_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "VCV2" (
    "id" TEXT NOT NULL,
    "seqid" INTEGER NOT NULL,
    "type" TEXT[],
    "issuer" TEXT NOT NULL,
    "issuanceDate" TEXT NOT NULL,
    "expirationDate" TEXT NOT NULL,
    "credential_schema" TEXT NOT NULL,
    "subject" TEXT NOT NULL,
    "subjectId" TEXT NOT NULL,
    "unsigned" JSONB,
    "signed" JSONB,
    "proof" JSONB,
    "status" "VCStatus" NOT NULL DEFAULT 'ISSUED',
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    "presentationsId" TEXT,

    CONSTRAINT "VCV2_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Counter" (
    "id" TEXT NOT NULL,
    "type_of_entity" TEXT NOT NULL DEFAULT 'Credential',
    "for_next_credential" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "Counter_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Presentations" (
    "id" TEXT NOT NULL,
    "type" TEXT[],
    "holder" JSONB NOT NULL,
    "proof" JSONB,
    "status" "VCStatus" NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "Presentations_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "VCV2_seqid_key" ON "VCV2"("seqid");

-- AddForeignKey
ALTER TABLE "VCV2" ADD CONSTRAINT "VCV2_presentationsId_fkey" FOREIGN KEY ("presentationsId") REFERENCES "Presentations"("id") ON DELETE SET NULL ON UPDATE CASCADE;
