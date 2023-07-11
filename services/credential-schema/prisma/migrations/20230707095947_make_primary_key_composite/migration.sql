/*
  Warnings:

  - The primary key for the `VerifiableCredentialSchema` table will be changed. If it partially fails, the table could be left without primary key constraint.

*/
-- AlterTable
ALTER TABLE "VerifiableCredentialSchema" DROP CONSTRAINT "VerifiableCredentialSchema_pkey",
ADD CONSTRAINT "VerifiableCredentialSchema_pkey" PRIMARY KEY ("id", "version");
