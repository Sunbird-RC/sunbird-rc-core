import { Test, TestingModule } from '@nestjs/testing';
import { VcController } from './vc.controller';
import VcService from './vc.service';
import { SignJsonDTO } from './dtos/Sign.dto';
import { VerifyJsonDTO } from './dtos/Verify.dto';

describe('VcController', () => {
  let controller: VcController;
  let service: VcService;

  beforeAll(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [VcController],
      providers: [
        {
          provide: VcService,
          useFactory: () => ({
            sign: jest.fn(),
            verify: jest.fn()
          }),
        }
      ],
    }).compile();

    controller = module.get<VcController>(VcController);
    service = module.get<VcService>(VcService);
  });

  beforeEach(async () => {
    jest.restoreAllMocks();
  })

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('sign', () => {
    it('should sign an unsigned VC', async () => {
      const body: SignJsonDTO = {
        DID: 'exampleDID',
        payload: { data: 'exampleData' },
      };

      const signedVc = 'signedVC';
      jest.spyOn(service, 'sign').mockImplementation(async () => signedVc);
      const result = await controller.sign(body);
      expect(result).toEqual(signedVc);
      expect(service.sign).toHaveBeenCalledWith(body.DID, body.payload);
    });
  });

  describe('verify', () => {
    it('should verify a signed VC', async () => {
      const body: VerifyJsonDTO = {
        DID: 'exampleDID',
        payload: { data: 'exampleData' },
      };

      const verificationResult = true;
      jest.spyOn(service, 'verify').mockImplementation(async () => verificationResult);

      const result = await controller.verify(body);
      expect(result).toEqual(verificationResult);
      expect(service.verify).toHaveBeenCalledWith(body.DID, body.payload);
    });
  });
});
