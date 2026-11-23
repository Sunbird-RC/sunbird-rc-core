/*
  Warnings:

  - Changed the type of `privateKey` on the `Identity` table. No cast exists, the column would be dropped and recreated, which cannot be done if there is data, since the column is required.

*/
-- AlterTable
-- Split into two statements: a single ALTER TABLE that drops and re-adds the same
-- column is rejected by PostgreSQL-compatible engines that apply subcommands
-- separately (e.g. YugabyteDB: "The column already exists: privateKey").
ALTER TABLE "Identity" DROP COLUMN "privateKey";
ALTER TABLE "Identity" ADD COLUMN     "privateKey" JSONB NOT NULL;
