/*
  Warnings:

  - The primary key for the `Identity` table will be changed. If it partially fails, the table could be left without primary key constraint.
  - You are about to drop the column `did` on the `Identity` table. All the data in the column will be lost.
  - Added the required column `id` to the `Identity` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "Identity" DROP CONSTRAINT "Identity_pkey",
DROP COLUMN "did",
ADD COLUMN     "id" TEXT NOT NULL,
ADD CONSTRAINT "Identity_pkey" PRIMARY KEY ("id");
