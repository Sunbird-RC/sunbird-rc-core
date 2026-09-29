import { Test, TestingModule } from '@nestjs/testing';
import { VcController } from './vc.controller';
import VcService from './vc.service';
import { JwtSignerService } from './jwt.service';
import { MdocService } from './mdoc.service';
import { SignJsonDTO } from './dtos/Sign.dto';
import { VerifyJsonDTO } from './dtos/Verify.dto';
import { SignJwtDTO, VerifyJwtDTO, SignSdJwtDTO, VerifySdJwtDTO } from './dtos/SignJwt.dto';

describe('VcController', () => {
  let controller: VcController;
  let service: VcService;
  let jwtSigner: JwtSignerService;
  let mdocSigner: MdocService;

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
        },
        {
          provide: JwtSignerService,
          useFactory: () => ({
            signJwt: jest.fn(),
            verifyJwt: jest.fn(),
            signSdJwt: jest.fn(),
            verifySdJwt: jest.fn(),
          }),
        },
        {
          provide: MdocService,
          useFactory: () => ({
            signMdoc: jest.fn(),
            verifyMdoc: jest.fn(),
          }),
        },
      ],
    }).compile();

    controller = module.get<VcController>(VcController);
    service = module.get<VcService>(VcService);
    jwtSigner = module.get<JwtSignerService>(JwtSignerService);
    mdocSigner = module.get<MdocService>(MdocService);
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

  describe('signJwt', () => {
    it('wraps the signed JWT in { jwt }', async () => {
      const body: SignJwtDTO = { DID: 'exampleDID', payload: { sub: 'holder' } };
      jest.spyOn(jwtSigner, 'signJwt').mockResolvedValue('signed.jwt.token');

      const result = await controller.signJwt(body);
      expect(result).toEqual({ jwt: 'signed.jwt.token' });
      expect(jwtSigner.signJwt).toHaveBeenCalledWith(body.DID, body.payload, {});
    });

    it('passes through an explicit header', async () => {
      const body: SignJwtDTO = { DID: 'exampleDID', payload: {}, header: { typ: 'JWT' } };
      jest.spyOn(jwtSigner, 'signJwt').mockResolvedValue('signed.jwt.token');

      await controller.signJwt(body);
      expect(jwtSigner.signJwt).toHaveBeenCalledWith(body.DID, body.payload, { typ: 'JWT' });
    });
  });

  describe('verifyJwt', () => {
    it('passes through the verification result', async () => {
      const body: VerifyJwtDTO = { jwt: 'some.jwt.token', DID: 'exampleDID' };
      const verifyResult = { verified: true, payload: { sub: 'holder' } };
      jest.spyOn(jwtSigner, 'verifyJwt').mockResolvedValue(verifyResult);

      const result = await controller.verifyJwt(body);
      expect(result).toEqual(verifyResult);
      expect(jwtSigner.verifyJwt).toHaveBeenCalledWith(body.jwt, body.DID);
    });
  });

  describe('signSdJwt', () => {
    it('wraps the signed SD-JWT in { sdJwt }', async () => {
      const body: SignSdJwtDTO = { DID: 'exampleDID', payload: { name: 'Alice' }, disclosable: ['name'] };
      jest.spyOn(jwtSigner, 'signSdJwt').mockResolvedValue('jws~disclosure~');

      const result = await controller.signSdJwt(body);
      expect(result).toEqual({ sdJwt: 'jws~disclosure~' });
      expect(jwtSigner.signSdJwt).toHaveBeenCalledWith(body.DID, body.payload, body.disclosable, {});
    });

    it('defaults disclosable to [] and header to {} when omitted', async () => {
      const body: SignSdJwtDTO = { DID: 'exampleDID', payload: { name: 'Alice' } };
      jest.spyOn(jwtSigner, 'signSdJwt').mockResolvedValue('jws~');

      await controller.signSdJwt(body);
      expect(jwtSigner.signSdJwt).toHaveBeenCalledWith(body.DID, body.payload, [], {});
    });
  });

  describe('verifySdJwt', () => {
    it('passes through the verification result', async () => {
      const body: VerifySdJwtDTO = { sdJwt: 'jws~disclosure~', DID: 'exampleDID' };
      const verifyResult = { verified: true, claims: { name: 'Alice' } };
      jest.spyOn(jwtSigner, 'verifySdJwt').mockResolvedValue(verifyResult);

      const result = await controller.verifySdJwt(body);
      expect(result).toEqual(verifyResult);
      expect(jwtSigner.verifySdJwt).toHaveBeenCalledWith(body.sdJwt, body.DID, body.keyBinding);
    });
  });

  describe('signMdoc', () => {
    it('wraps the signed mdoc in { mdoc }', async () => {
      const body = {
        DID: 'exampleDID',
        docType: 'org.iso.18013.5.1.mDL',
        namespaces: { 'org.iso.18013.5.1': { given_name: 'Alice' } },
      };
      jest.spyOn(mdocSigner, 'signMdoc').mockResolvedValue('encoded-mdoc');

      const result = await controller.signMdoc(body);
      expect(result).toEqual({ mdoc: 'encoded-mdoc' });
      expect(mdocSigner.signMdoc).toHaveBeenCalledWith(body.DID, body.docType, body.namespaces, undefined);
    });

    it('passes through an explicit deviceKeyJwk', async () => {
      const body = {
        DID: 'exampleDID',
        docType: 'org.iso.18013.5.1.mDL',
        namespaces: {},
        deviceKeyJwk: { kty: 'EC' },
      };
      jest.spyOn(mdocSigner, 'signMdoc').mockResolvedValue('encoded-mdoc');

      await controller.signMdoc(body);
      expect(mdocSigner.signMdoc).toHaveBeenCalledWith(body.DID, body.docType, body.namespaces, body.deviceKeyJwk);
    });
  });

  describe('verifyMdoc', () => {
    it('passes through the verification result', async () => {
      const body = { mdoc: 'encoded-mdoc' };
      const verifyResult = { verified: true, claims: { 'org.iso.18013.5.1': { given_name: 'Alice' } } };
      jest.spyOn(mdocSigner, 'verifyMdoc').mockResolvedValue(verifyResult);

      const result = await controller.verifyMdoc(body);
      expect(result).toEqual(verifyResult);
      expect(mdocSigner.verifyMdoc).toHaveBeenCalledWith(body.mdoc);
    });
  });
});
