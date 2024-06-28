import { Test, TestingModule } from '@nestjs/testing';
import { AppController } from './app.controller';
import { HealthCheckService, HttpHealthIndicator } from '@nestjs/terminus';
import { PrismaHealthIndicator } from './utils/prisma.health';

describe('AppController', () => {
  let appController: AppController;
  let healthCheckService: HealthCheckService;
  let prismaIndicator: PrismaHealthIndicator;
  let http: HttpHealthIndicator;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [AppController],
      providers: [
        {
          provide: HealthCheckService,
          useValue: {
            check: jest.fn(),
          },
        },
        {
          provide: PrismaHealthIndicator,
          useValue: {
            isHealthy: jest.fn(),
          },
        },
        {
          provide: HttpHealthIndicator,
          useValue: {
            responseCheck: jest.fn(),
          },
        },
      ],
    }).compile();

    appController = module.get<AppController>(AppController);
    healthCheckService = module.get<HealthCheckService>(HealthCheckService);
    prismaIndicator = module.get<PrismaHealthIndicator>(PrismaHealthIndicator);
    http = module.get<HttpHealthIndicator>(HttpHealthIndicator);
  });

  it('should be defined', () => {
    expect(appController).toBeDefined();
  });

  describe('checkHealth', () => {
    it('should return health check result', async () => {
      const result = { status: 'ok', info: {}, error: {}, details: {} };
      (healthCheckService.check as jest.Mock).mockResolvedValue(result);
      (prismaIndicator.isHealthy as jest.Mock).mockResolvedValue({ db: { status: 'up' } });
      (http.responseCheck as jest.Mock).mockResolvedValue({ 'identity-service': { status: 'up' } });

      expect(await appController.checkHealth()).toBe(result);
      expect(healthCheckService.check).toHaveBeenCalledWith([
        expect.any(Function),
        expect.any(Function),
      ]);
    });

    it('should call prismaIndicator.isHealthy', async () => {
      (healthCheckService.check as jest.Mock).mockImplementation(async (indicators) => {
        await indicators[0]();
        await indicators[1]();
      });

      await appController.checkHealth();
      expect(prismaIndicator.isHealthy).toHaveBeenCalledWith('db');
    });

    it('should call http.responseCheck with correct arguments', async () => {
      process.env.IDENTITY_BASE_URL = 'http://identity-service-url';
      (healthCheckService.check as jest.Mock).mockImplementation(async (indicators) => {
        await indicators[0]();
        await indicators[1]();
      });

      await appController.checkHealth();
      expect(http.responseCheck).toHaveBeenCalledWith(
        'identity-service',
        'http://identity-service-url/health',
        expect.any(Function)
      );
    });
  });
});
