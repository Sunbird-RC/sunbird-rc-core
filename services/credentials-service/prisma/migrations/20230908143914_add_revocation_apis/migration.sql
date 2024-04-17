-- CreateTable
CREATE TABLE "RevocationLists" (
    "issuer" TEXT NOT NULL,
    "latestRevocationListId" TEXT NOT NULL,
    "lastCredentialIdx" INTEGER NOT NULL,
    "allRevocationLists" TEXT[],

    CONSTRAINT "RevocationLists_pkey" PRIMARY KEY ("issuer")
);
