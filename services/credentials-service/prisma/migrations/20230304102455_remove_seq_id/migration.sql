/*
  Warnings:

  - You are about to drop the column `seqid` on the `VCV2` table. All the data in the column will be lost.

*/
-- DropIndex
DROP INDEX "VCV2_seqid_key";

-- AlterTable
ALTER TABLE "VCV2" DROP COLUMN "seqid";
