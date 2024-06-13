
import { PrismaService } from './prisma.service';
import { HealthCheckError } from '@nestjs/terminus';
import { PrismaHealthIndicator } from 'src/utils/prisma.health';

describe('PrismaHealthIndicator', () => {
  let prismaHealthIndicator: PrismaHealthIndicator;
  let prismaService: PrismaService;

  beforeAll(() => {
    prismaService = new PrismaService(); // Mock or use a real instance of PrismaService here
    prismaHealthIndicator = new PrismaHealthIndicator(prismaService);
  });

  beforeEach(() => {
    jest.restoreAllMocks();
  })

  it('should be defined', () => {
    expect(prismaHealthIndicator).toBeDefined();
  });

  describe('isHealthy', () => {
    it('should return healthy result if PrismaService query is successful', async () => {
      jest.spyOn(prismaService, '$queryRaw').mockResolvedValueOnce([1]); // Mock successful query response

      const result = await prismaHealthIndicator.isHealthy('prisma');

      expect(result).toEqual({ prisma: { status: 'up' } });
      expect(prismaService.$queryRaw).toHaveBeenCalledWith(expect.anything()); // Assert that $queryRaw was called
    });

    it('should throw HealthCheckError if PrismaService query fails', async () => {
      jest.spyOn(prismaService, '$queryRaw').mockRejectedValueOnce(new Error('Prisma query failed')); // Mock failed query response

      await expect(prismaHealthIndicator.isHealthy('prisma')).rejects.toThrowError(HealthCheckError);
      expect(prismaService.$queryRaw).toHaveBeenCalledWith(expect.anything()); // Assert that $queryRaw was called
    });
  });
});
