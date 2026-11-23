/*
  Warnings:

  - Changed the type of `version` on the `VerifiableCredentialSchema` table. No cast exists, the column would be dropped and recreated, which cannot be done if there is data, since the column is required.
  - Changed the type of `version` on the `VerifiablePresentationSchema` table. No cast exists, the column would be dropped and recreated, which cannot be done if there is data, since the column is required.

*/
-- AlterTable
-- Split into two statements: a single ALTER TABLE that drops and re-adds the same
-- column is rejected by PostgreSQL-compatible engines that apply subcommands
-- separately (e.g. YugabyteDB: "The column already exists: version").
ALTER TABLE "VerifiableCredentialSchema" DROP COLUMN "version";
ALTER TABLE "VerifiableCredentialSchema" ADD COLUMN     "version" INTEGER NOT NULL;

-- AlterTable
ALTER TABLE "VerifiablePresentationSchema" DROP COLUMN "version";
ALTER TABLE "VerifiablePresentationSchema" ADD COLUMN     "version" INTEGER NOT NULL;
