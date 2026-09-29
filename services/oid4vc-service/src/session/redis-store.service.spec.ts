const mockRedisInstance: any = {
  set: jest.fn(),
  get: jest.fn(),
  del: jest.fn(),
  getdel: jest.fn(),
  multi: jest.fn(),
  on: jest.fn(),
};

jest.mock('ioredis', () => {
  return {
    __esModule: true,
    default: jest.fn().mockImplementation(() => mockRedisInstance),
  };
});

import { RedisStoreService } from './redis-store.service';

describe('RedisStoreService', () => {
  const ORIGINAL_ENV = process.env;
  let service: RedisStoreService;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    jest.clearAllMocks();
    service = new RedisStoreService();
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  it('registers an error handler on construction', () => {
    expect(mockRedisInstance.on).toHaveBeenCalledWith('error', expect.any(Function));
  });

  describe('set', () => {
    it('calls redis.set with a JSON-stringified value and EX ttl', async () => {
      mockRedisInstance.set.mockResolvedValue('OK');
      await service.set('k', { a: 1 }, 60);
      expect(mockRedisInstance.set).toHaveBeenCalledWith('k', JSON.stringify({ a: 1 }), 'EX', 60);
    });
  });

  describe('get', () => {
    it('parses and returns JSON when a value exists', async () => {
      mockRedisInstance.get.mockResolvedValue(JSON.stringify({ a: 1 }));
      const result = await service.get('k');
      expect(mockRedisInstance.get).toHaveBeenCalledWith('k');
      expect(result).toEqual({ a: 1 });
    });

    it('returns null when redis.get resolves null', async () => {
      mockRedisInstance.get.mockResolvedValue(null);
      const result = await service.get('k');
      expect(result).toBeNull();
    });
  });

  describe('del', () => {
    it('calls redis.del', async () => {
      mockRedisInstance.del.mockResolvedValue(1);
      await service.del('k');
      expect(mockRedisInstance.del).toHaveBeenCalledWith('k');
    });
  });

  describe('getdel', () => {
    it('uses the native getdel and parses its result', async () => {
      mockRedisInstance.getdel.mockResolvedValue(JSON.stringify({ a: 1 }));
      const result = await service.getdel('k');
      expect(mockRedisInstance.getdel).toHaveBeenCalledWith('k');
      expect(result).toEqual({ a: 1 });
    });

    it('returns null when the native getdel finds no key', async () => {
      mockRedisInstance.getdel.mockResolvedValue(null);
      const result = await service.getdel('k');
      expect(result).toBeNull();
    });

    it('falls back to a multi().get().del().exec() transaction when native getdel rejects', async () => {
      mockRedisInstance.getdel.mockRejectedValue(new Error('ERR unknown command'));
      const multiChain = {
        get: jest.fn().mockReturnThis(),
        del: jest.fn().mockReturnThis(),
        exec: jest.fn().mockResolvedValue([[null, JSON.stringify({ a: 1 })]]),
      };
      mockRedisInstance.multi.mockReturnValue(multiChain);

      const result = await service.getdel('k');

      expect(mockRedisInstance.multi).toHaveBeenCalled();
      expect(multiChain.get).toHaveBeenCalledWith('k');
      expect(multiChain.del).toHaveBeenCalledWith('k');
      expect(result).toEqual({ a: 1 });
    });

    it('fallback path returns null when the key does not exist', async () => {
      mockRedisInstance.getdel.mockRejectedValue(new Error('ERR unknown command'));
      const multiChain = {
        get: jest.fn().mockReturnThis(),
        del: jest.fn().mockReturnThis(),
        exec: jest.fn().mockResolvedValue([[null, null]]),
      };
      mockRedisInstance.multi.mockReturnValue(multiChain);

      const result = await service.getdel('k');
      expect(result).toBeNull();
    });
  });
});