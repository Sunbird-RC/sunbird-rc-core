/*
  Warnings:

  - Changed the type of `subject` on the `VCV2` table. No cast exists, the column would be dropped and recreated, which cannot be done if there is data, since the column is required.

*/
-- AlterTable
ALTER TABLE "VCV2" DROP COLUMN "subject",
ADD COLUMN     "subject" JSONB;
