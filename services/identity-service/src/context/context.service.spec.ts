import { HttpModule } from '@nestjs/axios';
import { Test, TestingModule } from '@nestjs/testing';
import { ContextService } from './context.service';
import { PrismaService } from '../utils/prisma.service';

describe('ContextService', () => {
  let service: ContextService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [ContextService, PrismaService],
      imports: [HttpModule]
    }).compile();

    service = module.get<ContextService>(ContextService);
    jest.restoreAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should throw exception on unable to save context to database', async () => {
    jest.spyOn((service as any).prisma.context, 'create')
    .mockImplementationOnce(() => Promise.reject("Unable to save context"));
    expect(service.saveContext({"version": "1.0.0" })).rejects
    .toThrow('Error writing context to database');
  })

  it('should throw exception on context not found', async () => {
    expect(service.getContextById("abcd")).rejects
    .toThrow('The context not found');
  })

  it('should save/retrieve/resolve a context', async () => {
    const contextToSave = { "version": "1.0.0" };
    const publicUrl = "http://localhost:3332/context";
    (service as any).publicEndpoint = publicUrl;
    const contextUrl = await service.saveContext(contextToSave);
    expect(contextUrl).toBeDefined();
    const id = contextUrl.substring(publicUrl.length + 1);
    expect(contextUrl).toContain(publicUrl);
    const context = await service.getContextById(id);
    expect(context).toEqual(contextToSave);
    jest.spyOn((service as any).httpService, 'axiosRef')
    .mockImplementationOnce(() => Promise.resolve({
            data: contextToSave,
            headers: {},
            config: { url: 'https://www.did.abc.com/context/123' },
            status: 200,
            statusText: 'OK',
          }));
    const resolved = await service.resolveContext([
        contextUrl,
        contextToSave,
        JSON.stringify(contextToSave),
        "https://www.did.abc.com/context/123"
    ]);
    expect(resolved).toHaveLength(4);
    expect(resolved[0]).toEqual(contextToSave);
    expect(resolved[1]).toEqual(contextToSave);
    expect(resolved[2]).toEqual(contextToSave);
    expect(resolved[3]).toEqual(contextToSave);
  });

  it("check save context and get urls", async() => {
    let contextToSave = [
        "abc.def.com",
        JSON.stringify({ "version": "1.0.0" }),
        {
            "version": "1.0.0"
        }
    ];
    const publicUrl = "http://localhost:3332/context";
    (service as any).publicEndpoint = publicUrl;
    const urls = await service.saveContextAndGetUrl(contextToSave);
    expect(urls).toHaveLength(3);
    expect(urls[0]).toEqual("abc.def.com");
    expect(urls[1]).toContain(publicUrl);
    expect(urls[2]).toContain(publicUrl);
  });

  it("should throw exception on saving an invalid exception", async() => {
    let contextToSave = 123;
    expect(service.saveContextAndGetUrl(contextToSave))
    .rejects.toThrow("Unable to process context");
  });

});
