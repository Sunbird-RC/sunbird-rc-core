import { HealthCheckError } from '@nestjs/terminus';
import { HealthCheckUtilsService } from './healthcheck.utils.service';

describe('HealthCheckUtilsService', () => {
  let service: HealthCheckUtilsService;
  let prisma: { $queryRaw: jest.Mock };
  let axiosRef: { get: jest.Mock };

  beforeEach(() => {
    prisma = { $queryRaw: jest.fn() };
    axiosRef = { get: jest.fn() };
    service = new HealthCheckUtilsService(prisma as any, { axiosRef } as any);
  });

  describe('prismaIsHealthy', () => {
    it('returns up when the query succeeds', async () => {
      prisma.$queryRaw.mockResolvedValue([{ '?column?': 1 }]);
      const result = await service.prismaIsHealthy('db');
      expect(result).toEqual({ db: { status: 'up' } });
    });

    it('throws HealthCheckError when the query fails', async () => {
      prisma.$queryRaw.mockRejectedValue(new Error('connection refused'));
      await expect(service.prismaIsHealthy('db')).rejects.toThrow(HealthCheckError);
      await expect(service.prismaIsHealthy('db')).rejects.toThrow('Prisma health check failed');
    });
  });

  describe('identityIsHealthy', () => {
    it('returns up when identity-service responds', async () => {
      axiosRef.get.mockResolvedValue({ status: 200 });
      const result = await service.identityIsHealthy('identity-service');
      expect(result).toEqual({ 'identity-service': { status: 'up' } });
      expect(axiosRef.get).toHaveBeenCalledWith(`${process.env.IDENTITY_BASE_URL}/health`);
    });

    it('throws HealthCheckError when identity-service is unreachable', async () => {
      axiosRef.get.mockRejectedValue(new Error('ECONNREFUSED'));
      await expect(service.identityIsHealthy('identity-service')).rejects.toThrow(HealthCheckError);
      await expect(service.identityIsHealthy('identity-service')).rejects.toThrow('Identity health check failed');
    });
  });

  describe('credSchemaIsHealthy', () => {
    it('returns up when credential-schema-service responds', async () => {
      axiosRef.get.mockResolvedValue({ status: 200 });
      const result = await service.credSchemaIsHealthy('credential-schema-service');
      expect(result).toEqual({ 'credential-schema-service': { status: 'up' } });
      expect(axiosRef.get).toHaveBeenCalledWith(`${process.env.SCHEMA_BASE_URL}/health`);
    });

    it('throws HealthCheckError when credential-schema-service is unreachable', async () => {
      axiosRef.get.mockRejectedValue(new Error('ECONNREFUSED'));
      await expect(service.credSchemaIsHealthy('credential-schema-service')).rejects.toThrow(HealthCheckError);
      await expect(service.credSchemaIsHealthy('credential-schema-service')).rejects.toThrow(
        'Credential Schema health check failed',
      );
    });
  });
});