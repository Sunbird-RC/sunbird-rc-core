/*
  Warnings:

  - Changed the type of `privateKey` on the `Identity` table. No cast exists, the column would be dropped and recreated, which cannot be done if there is data, since the column is required.

*/
-- AlterTable
ALTER TABLE "Identity" DROP COLUMN "privateKey",
ADD COLUMN     "privateKey" JSONB NOT NULL;
