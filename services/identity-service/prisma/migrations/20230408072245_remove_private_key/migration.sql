/*
  Warnings:

  - You are about to drop the column `privateKey` on the `Identity` table. All the data in the column will be lost.

*/
-- AlterTable
ALTER TABLE "Identity" DROP COLUMN "privateKey";
