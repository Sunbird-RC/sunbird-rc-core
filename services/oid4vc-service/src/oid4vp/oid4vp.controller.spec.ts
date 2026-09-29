// oid4vp.controller.ts imports oid4vp.service.ts, which transitively imports
// @auth0/mdl (via mdoc-presentation.util.ts) for the mso_mdoc presentation
// path this suite doesn't exercise. Its cose-kit dependency uses a Node
// "imports" subpath (#runtime/pkijs.js) jest's resolver can't load — stub it
// out rather than pull the whole COSE/CBOR chain in. Same workaround as
// oid4vp.service.spec.ts.
jest.mock('@auth0/mdl', () => ({ Verifier: class {} }), { virtual: true });
jest.mock('@auth0/mdl/lib/cbor', () => ({ cborEncode: jest.fn(), DataItem: {} }), { virtual: true });

import { NotAcceptableException } from '@nestjs/common';
import { Oid4vpController } from './oid4vp.controller';

describe('Oid4vpController', () => {
  let oid4vp: any;
  let controller: Oid4vpController;

  beforeEach(() => {
    oid4vp = {
      createRequest: jest.fn(),
      getRequestObject: jest.fn(),
      submitResponse: jest.fn(),
      getStatus: jest.fn(),
    };
    controller = new Oid4vpController(oid4vp);
  });

  describe('createRequest', () => {
    it('delegates to oid4vp.createRequest with the body', () => {
      const body = { dcql_query: {} };
      const expected = { qr_data: 'openid4vp://' };
      oid4vp.createRequest.mockReturnValue(expected);

      const result = controller.createRequest(body);
      expect(result).toBe(expected);
      expect(oid4vp.createRequest).toHaveBeenCalledWith(body);
    });

    it('defaults a null/undefined body to {}', () => {
      controller.createRequest(undefined);
      expect(oid4vp.createRequest).toHaveBeenCalledWith({});
    });
  });

  describe('getRequestObject', () => {
    it('passes through and sets the content-type header when no accept header is present', async () => {
      const res = { header: jest.fn() };
      oid4vp.getRequestObject.mockResolvedValue({ body: { foo: 'bar' }, contentType: 'application/json' });

      const result = await controller.getRequestObject('req-1', undefined, res as any);
      expect(result).toEqual({ foo: 'bar' });
      expect(oid4vp.getRequestObject).toHaveBeenCalledWith('req-1');
      expect(res.header).toHaveBeenCalledWith('content-type', 'application/json');
    });

    it('passes through when accept is */*', async () => {
      const res = { header: jest.fn() };
      oid4vp.getRequestObject.mockResolvedValue({ body: 'signed.jwt', contentType: 'application/oauth-authz-req+jwt' });

      const result = await controller.getRequestObject('req-1', '*/*', res as any);
      expect(result).toBe('signed.jwt');
      expect(res.header).toHaveBeenCalledWith('content-type', 'application/oauth-authz-req+jwt');
    });

    it('passes through when accept matches the actual content type exactly', async () => {
      const res = { header: jest.fn() };
      oid4vp.getRequestObject.mockResolvedValue({ body: { foo: 'bar' }, contentType: 'application/json' });

      const result = await controller.getRequestObject('req-1', 'application/json', res as any);
      expect(result).toEqual({ foo: 'bar' });
      expect(res.header).toHaveBeenCalledWith('content-type', 'application/json');
    });

    it('throws NotAcceptableException when accept does not match and is not */*', async () => {
      const res = { header: jest.fn() };
      oid4vp.getRequestObject.mockResolvedValue({ body: { foo: 'bar' }, contentType: 'application/json' });

      await expect(
        controller.getRequestObject('req-1', 'application/oauth-authz-req+jwt', res as any),
      ).rejects.toThrow(NotAcceptableException);
      expect(res.header).not.toHaveBeenCalled();
    });
  });

  describe('submitResponse', () => {
    it('delegates to oid4vp.submitResponse with the body', () => {
      const body = { vp_token: 'token' };
      const expected = { status: 'ok' };
      oid4vp.submitResponse.mockReturnValue(expected);

      const result = controller.submitResponse(body);
      expect(result).toBe(expected);
      expect(oid4vp.submitResponse).toHaveBeenCalledWith(body);
    });

    it('defaults a null/undefined body to {}', () => {
      controller.submitResponse(undefined);
      expect(oid4vp.submitResponse).toHaveBeenCalledWith({});
    });
  });

  describe('getStatus', () => {
    it('delegates to oid4vp.getStatus with the id', () => {
      const expected = { status: 'verified' };
      oid4vp.getStatus.mockReturnValue(expected);

      const result = controller.getStatus('tx-1');
      expect(result).toBe(expected);
      expect(oid4vp.getStatus).toHaveBeenCalledWith('tx-1');
    });
  });
});