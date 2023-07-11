/*
  Warnings:

  - Changed the type of `version` on the `VerifiableCredentialSchema` table. No cast exists, the column would be dropped and recreated, which cannot be done if there is data, since the column is required.
  - Changed the type of `version` on the `VerifiablePresentationSchema` table. No cast exists, the column would be dropped and recreated, which cannot be done if there is data, since the column is required.

*/
-- AlterTable
ALTER TABLE "VerifiableCredentialSchema" DROP COLUMN "version",
ADD COLUMN     "version" INTEGER NOT NULL;

-- AlterTable
ALTER TABLE "VerifiablePresentationSchema" DROP COLUMN "version",
ADD COLUMN     "version" INTEGER NOT NULL;
