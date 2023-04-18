/*
  Warnings:

  - Added the required column `didDoc` to the `Identity` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "Identity" ADD COLUMN     "didDoc" JSONB NOT NULL;
