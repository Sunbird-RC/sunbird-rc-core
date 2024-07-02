import { Test, TestingModule } from '@nestjs/testing';
import { UtilsService } from './utils.service';
import { HttpService } from '@nestjs/axios';
import { HttpException, InternalServerErrorException, Logger } from '@nestjs/common';
import { of, throwError } from 'rxjs';

describe('UtilsService', () => {
  let utilsService: UtilsService;
  let httpService: HttpService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UtilsService,
        {
          provide: HttpService,
          useValue: {
            axiosRef: {
              post: jest.fn(),
            },
          },
        },
      ],
    }).compile();

    utilsService = module.get<UtilsService>(UtilsService);
    httpService = module.get<HttpService>(HttpService);
  });

  it('should be defined', () => {
    expect(utilsService).toBeDefined();
  });

  describe('sign', () => {
    it('should return signed VC response data', async () => {
      const did = 'did:example:123';
      const body = { some: 'payload' };
      const signedVCResponse = { data: { signed: 'data' } };
      (httpService.axiosRef.post as jest.Mock).mockResolvedValue(signedVCResponse);

      const result = await utilsService.sign(did, body);
      expect(result).toEqual(signedVCResponse.data);
      expect(httpService.axiosRef.post).toHaveBeenCalledWith(
        `${process.env.IDENTITY_BASE_URL}/utils/sign`,
        { DID: did, payload: body }
      );
    });

    it('should throw HttpException when request fails', async () => {
      const did = 'did:example:123';
      const body = { some: 'payload' };
      const error = new Error('Request failed');
      (error as any).response = { data: 'Error data' };
      (httpService.axiosRef.post as jest.Mock).mockRejectedValue(error);

      await expect(utilsService.sign(did, body)).rejects.toThrow(HttpException);
      await expect(utilsService.sign(did, body)).rejects.toThrow("Couldn't sign the schema");
    });
  });

  describe('generateDID', () => {
    it('should return generated DID', async () => {
      const body = { some: 'payload' };
      const didResponse = { data: ['did:example:123'] };
      (httpService.axiosRef.post as jest.Mock).mockResolvedValue(didResponse);

      const result = await utilsService.generateDID(body);
      expect(result).toEqual(didResponse.data[0]);
      expect(httpService.axiosRef.post).toHaveBeenCalledWith(
        `${process.env.IDENTITY_BASE_URL}/did/generate`,
        body
      );
    });

    it('should throw InternalServerErrorException when request fails', async () => {
      const body = { some: 'payload' };
      const error = new Error('Request failed');
      (httpService.axiosRef.post as jest.Mock).mockRejectedValue(error);

      await expect(utilsService.generateDID(body)).rejects.toThrow(InternalServerErrorException);
      await expect(utilsService.generateDID(body)).rejects.toThrow('Can not generate a new DID');
    });
  });
});
