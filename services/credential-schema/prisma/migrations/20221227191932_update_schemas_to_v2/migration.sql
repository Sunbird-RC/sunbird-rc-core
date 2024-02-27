/*
  Warnings:

  - You are about to drop the column `description` on the `VerifiableCredentialSchema` table. All the data in the column will be lost.
  - You are about to drop the column `description` on the `VerifiablePresentationSchema` table. All the data in the column will be lost.
  - Added the required column `author` to the `VerifiableCredentialSchema` table without a default value. This is not possible if the table is not empty.
  - Added the required column `authored` to the `VerifiableCredentialSchema` table without a default value. This is not possible if the table is not empty.
  - Added the required column `proof` to the `VerifiableCredentialSchema` table without a default value. This is not possible if the table is not empty.
  - Added the required column `author` to the `VerifiablePresentationSchema` table without a default value. This is not possible if the table is not empty.
  - Added the required column `authored` to the `VerifiablePresentationSchema` table without a default value. This is not possible if the table is not empty.
  - Added the required column `proof` to the `VerifiablePresentationSchema` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "VerifiableCredentialSchema" DROP COLUMN "description",
ADD COLUMN     "author" TEXT NOT NULL,
ADD COLUMN     "authored" TIMESTAMP(3) NOT NULL,
ADD COLUMN     "proof" JSONB NOT NULL;

-- AlterTable
ALTER TABLE "VerifiablePresentationSchema" DROP COLUMN "description",
ADD COLUMN     "author" TEXT NOT NULL,
ADD COLUMN     "authored" TIMESTAMP(3) NOT NULL,
ADD COLUMN     "proof" JSONB NOT NULL;
