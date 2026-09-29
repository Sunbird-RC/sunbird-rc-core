import { Test, TestingModule } from '@nestjs/testing';
import { AppController } from './app.controller';
import { HealthCheckService } from '@nestjs/terminus';
import { HealthCheckUtilsService } from './credentials/utils/healthcheck.utils.service';

describe('AppController', () => {
  let controller: AppController;
  let healthCheckService: { check: jest.Mock };
  let healthCheckUtils: { [K in keyof HealthCheckUtilsService]?: jest.Mock };

  beforeEach(async () => {
    healthCheckService = { check: jest.fn() };
    healthCheckUtils = {
      prismaIsHealthy: jest.fn(),
      identityIsHealthy: jest.fn(),
      credSchemaIsHealthy: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [AppController],
      providers: [
        { provide: HealthCheckService, useValue: healthCheckService },
        { provide: HealthCheckUtilsService, useValue: healthCheckUtils },
      ],
    }).compile();

    controller = module.get<AppController>(AppController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('delegates to healthCheckService.check with the db, identity, and schema indicators', async () => {
    const expectedResult = { status: 'ok', info: {}, error: {}, details: {} };
    healthCheckService.check.mockImplementation(async (indicators: Array<() => Promise<any>>) => {
      await Promise.all(indicators.map((indicator) => indicator()));
      return expectedResult;
    });
    healthCheckUtils.prismaIsHealthy.mockResolvedValue({ db: { status: 'up' } });
    healthCheckUtils.identityIsHealthy.mockResolvedValue({ 'identity-service': { status: 'up' } });
    healthCheckUtils.credSchemaIsHealthy.mockResolvedValue({ 'credential-schema-service': { status: 'up' } });

    const result = await controller.checkHealth();

    expect(result).toBe(expectedResult);
    expect(healthCheckService.check).toHaveBeenCalledWith([
      expect.any(Function),
      expect.any(Function),
      expect.any(Function),
    ]);
    expect(healthCheckUtils.prismaIsHealthy).toHaveBeenCalledWith('db');
    expect(healthCheckUtils.identityIsHealthy).toHaveBeenCalledWith('identity-service');
    expect(healthCheckUtils.credSchemaIsHealthy).toHaveBeenCalledWith('credential-schema-service');
  });

  it('propagates a rejection when healthCheckService.check fails (degraded/unhealthy)', async () => {
    const error = new Error('degraded');
    healthCheckService.check.mockRejectedValue(error);

    await expect(controller.checkHealth()).rejects.toThrow('degraded');
  });
});