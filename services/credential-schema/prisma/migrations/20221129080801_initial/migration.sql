-- CreateTable
CREATE TABLE "VerifiableCredentialSchema" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "version" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "schema" JSONB NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    "deletedAt" TIMESTAMP(3),
    "tags" TEXT[],

    CONSTRAINT "VerifiableCredentialSchema_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "VerifiablePresentationSchema" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "version" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "schema" JSONB NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    "deletedAt" TIMESTAMP(3),
    "tags" TEXT[],

    CONSTRAINT "VerifiablePresentationSchema_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "VerifiableCredentialSchema_type_idx" ON "VerifiableCredentialSchema" USING HASH ("type");

-- CreateIndex
CREATE INDEX "VerifiableCredentialSchema_name_idx" ON "VerifiableCredentialSchema" USING HASH ("name");

-- CreateIndex
CREATE INDEX "VerifiablePresentationSchema_type_idx" ON "VerifiablePresentationSchema" USING HASH ("type");

-- CreateIndex
CREATE INDEX "VerifiablePresentationSchema_name_idx" ON "VerifiablePresentationSchema" USING HASH ("name");
