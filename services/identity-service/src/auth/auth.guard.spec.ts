import { Test, TestingModule } from '@nestjs/testing';
import { AuthGuard } from './auth.guard';
import { Reflector } from '@nestjs/core';
import { ConfigService } from '@nestjs/config';

jest.mock('jwks-rsa', () => ({
  __esModule: true,
  default: jest.fn(),
}));

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let reflector: Reflector;
  let configService: ConfigService;
  let originalEnv: NodeJS.ProcessEnv;

  beforeAll(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthGuard,
        {
          provide: Reflector,
          useValue: {
            get: jest.fn(),
          },
        },
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn(),
          },
        },
      ],
    }).compile();

    guard = module.get<AuthGuard>(AuthGuard);
    reflector = module.get<Reflector>(Reflector);
    configService = module.get<ConfigService>(ConfigService);
  });

  beforeEach(async () => {
    originalEnv = { ...process.env };
    process.env.ENABLE_AUTH = 'true';
    jest.restoreAllMocks();
  })

  it('should be defined', () => {
    expect(guard).toBeDefined();
  });

  describe('canActivate', () => {
    it('should return true if isPublic is set to true', async () => {
      jest.spyOn(reflector, 'get').mockReturnValue(true);
      const result = await guard.canActivate({ getHandler: jest.fn() });
      expect(result).toEqual(true);
    });

    it('should return true if ENABLE_AUTH is false', async () => {
      process.env.ENABLE_AUTH = 'false';
      jest.spyOn(reflector, 'get').mockReturnValue(false);
      jest.spyOn(configService, 'get').mockReturnValue('false');
      const result = await guard.canActivate({ getHandler: jest.fn() });
      expect(result).toEqual(true);
    });

    it('should return false if no Bearer token found', async () => {
      jest.spyOn(reflector, 'get').mockReturnValue(false);
      jest.spyOn(configService, 'get').mockReturnValue('true');
      const request = { headers: { authorization: 'InvalidToken' } };
      const result = await guard.canActivate({ getHandler: jest.fn(), switchToHttp: () => ({ getRequest: () => request }) });
      expect(result).toEqual(false);
    });

    // Add more test cases as needed to cover different scenarios
  });

  afterEach(() => {
    // Restore the original process.env after the test
    process.env = { ...originalEnv };
  });
});
