import { Test, TestingModule } from '@nestjs/testing';
import { PrismaHealthIndicator } from './prisma.health';
import { HealthCheckError } from '@nestjs/terminus';
import { PrismaClient } from '@prisma/client';

describe('PrismaHealthIndicator', () => {
  let prismaHealthIndicator: PrismaHealthIndicator;
  let prismaClient: PrismaClient;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        PrismaHealthIndicator,
        {
          provide: PrismaClient,
          useValue: {
            $queryRaw: jest.fn(),
          },
        },
      ],
    }).compile();

    prismaHealthIndicator = module.get<PrismaHealthIndicator>(PrismaHealthIndicator);
    prismaClient = module.get<PrismaClient>(PrismaClient);
  });

  it('should be defined', () => {
    expect(prismaHealthIndicator).toBeDefined();
  });

  describe('isHealthy', () => {
    it('should return health status as up', async () => {
      (prismaClient.$queryRaw as jest.Mock).mockResolvedValue([1]);

      const result = await prismaHealthIndicator.isHealthy('db');
      expect(result).toEqual({ db: { status: 'up' } });
      expect(prismaClient.$queryRaw).toHaveBeenCalledWith([`SELECT 1`]);
    });

    it('should throw HealthCheckError when query fails', async () => {
      const error = new Error('Query failed');
      (prismaClient.$queryRaw as jest.Mock).mockRejectedValue(error);

      await expect(prismaHealthIndicator.isHealthy('db')).rejects.toThrow(HealthCheckError);
      await expect(prismaHealthIndicator.isHealthy('db')).rejects.toThrow('Prisma health check failed');
    });
  });
});
