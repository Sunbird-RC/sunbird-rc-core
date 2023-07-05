import { PrismaClient } from '@prisma/client';

export async function setupTestValue() {
  const prisma = new PrismaClient();

  // Seed your test value using Prisma
  await prisma.identity.create({
    data: {
      id: 'did:test:123',
      didDoc: JSON.stringify({}),
      blockchainStatus: false,
    }
  });

  // Close the Prisma connection
  await prisma.$disconnect();
}