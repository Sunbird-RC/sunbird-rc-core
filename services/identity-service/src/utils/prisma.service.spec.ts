import { INestApplication } from '@nestjs/common';
import { PrismaService } from './prisma.service';

describe('PrismaService', () => {
  let prismaService: PrismaService;

  beforeEach(() => {
    prismaService = new PrismaService();
  });

  afterEach(async () => {
    await prismaService.$disconnect(); // Disconnect after each test to avoid connection leaks
  });

  it('should be defined', () => {
    expect(prismaService).toBeDefined();
  });

  describe('onModuleInit', () => {
    it('connects to the database', async () => {
      const connectSpy = jest.spyOn(prismaService, '$connect').mockResolvedValue(undefined);
      await expect(prismaService.onModuleInit()).resolves.not.toThrow();
      expect(connectSpy).toHaveBeenCalled();
    });

    it('propagates a connection failure', async () => {
      jest.spyOn(prismaService, '$connect').mockRejectedValue(new Error('connection refused'));
      await expect(prismaService.onModuleInit()).rejects.toThrow('connection refused');
    });
  });

  describe('enableShutdownHooks', () => {
    it('should enable shutdown hooks for the application', async () => {
      const mockApp = { close: jest.fn() } as unknown as INestApplication; // Mock INestApplication methods
      await prismaService.enableShutdownHooks(mockApp);

      // @ts-ignore
      process.emit("beforeExit");

      expect(mockApp.close).toHaveBeenCalled(); // Assert that app.close() was called
    });
  });
});
