/*
  Warnings:

  - You are about to drop the column `schema` on the `Template` table. All the data in the column will be lost.
  - Added the required column `schemaId` to the `Template` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "Template" DROP COLUMN "schema",
ADD COLUMN     "createdBy" TEXT,
ADD COLUMN     "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN     "schemaId" TEXT NOT NULL,
ADD COLUMN     "updatedBy" TEXT,
ADD COLUMN     "updated_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP;
