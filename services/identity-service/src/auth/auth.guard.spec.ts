import { Test, TestingModule } from '@nestjs/testing';
import { AuthGuard } from './auth.guard';
import { Reflector } from '@nestjs/core';
import { ConfigService } from '@nestjs/config';
import * as jwt from 'jsonwebtoken';
import * as crypto from 'crypto';

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
      const request = { headers: { } };
      const result = await guard.canActivate({ getHandler: jest.fn(), switchToHttp: () => ({ getRequest: () => request }) });
      expect(result).toEqual(false);
    });

    describe('with a real Bearer token', () => {
      let publicKeyPem: string;
      let privateKeyPem: string;
      const kid = 'test-kid-1';

      beforeAll(() => {
        const { publicKey, privateKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
        publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' }) as string;
        privateKeyPem = privateKey.export({ type: 'pkcs8', format: 'pem' }) as string;
      });

      function requestWithBearer(token: string) {
        return {
          getHandler: jest.fn(),
          switchToHttp: () => ({
            getRequest: () => ({ headers: { authorization: `Bearer ${token}` } }),
          }),
        };
      }

      beforeEach(() => {
        jest.spyOn(reflector, 'get').mockReturnValue(false);
      });

      it('returns true for a token signed by the key the JWKS endpoint resolves', async () => {
        (guard as any).client = {
          getSigningKey: (_kid: string, cb: any) => cb(null, { publicKey: publicKeyPem }),
        };
        const token = jwt.sign({ sub: 'user-1' }, privateKeyPem, { algorithm: 'RS256', keyid: kid });

        const result = await guard.canActivate(requestWithBearer(token));
        expect(result).toBe(true);
      });

      it('returns false for an expired token', async () => {
        (guard as any).client = {
          getSigningKey: (_kid: string, cb: any) => cb(null, { publicKey: publicKeyPem }),
        };
        const token = jwt.sign({ sub: 'user-1' }, privateKeyPem, { algorithm: 'RS256', keyid: kid, expiresIn: -10 });

        const result = await guard.canActivate(requestWithBearer(token));
        expect(result).toBe(false);
      });

      it('returns false for a token signed by a different key than the one JWKS resolves', async () => {
        const { publicKey: otherPublicKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
        (guard as any).client = {
          getSigningKey: (_kid: string, cb: any) =>
            cb(null, { publicKey: otherPublicKey.export({ type: 'spki', format: 'pem' }) }),
        };
        const token = jwt.sign({ sub: 'user-1' }, privateKeyPem, { algorithm: 'RS256', keyid: kid });

        const result = await guard.canActivate(requestWithBearer(token));
        expect(result).toBe(false);
      });

      it('returns false when the JWKS client fails to resolve a signing key', async () => {
        (guard as any).client = {
          getSigningKey: (_kid: string, cb: any) => cb(new Error('key not found'), null),
        };
        const token = jwt.sign({ sub: 'user-1' }, privateKeyPem, { algorithm: 'RS256', keyid: kid });

        const result = await guard.canActivate(requestWithBearer(token));
        expect(result).toBe(false);
      });
    });
  });

  afterEach(() => {
    // Restore the original process.env after the test
    process.env = { ...originalEnv };
  });
});
