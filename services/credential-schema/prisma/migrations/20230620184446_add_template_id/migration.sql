/*
  Warnings:

  - The primary key for the `Template` table will be changed. If it partially fails, the table could be left without primary key constraint.
  - You are about to drop the column `id` on the `Template` table. All the data in the column will be lost.
  - The required column `templateId` was added to the `Template` table with a prisma-level default value. This is not possible if the table is not empty. Please add this column as optional, then populate it before making it required.

*/
-- AlterTable
ALTER TABLE "Template" DROP CONSTRAINT "Template_pkey",
DROP COLUMN "id",
ADD COLUMN     "templateId" TEXT NOT NULL,
ADD CONSTRAINT "Template_pkey" PRIMARY KEY ("templateId");
