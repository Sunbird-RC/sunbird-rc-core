import { Oid4vciController } from './oid4vci.controller';

describe('Oid4vciController', () => {
  let oid4vci: any;
  let controller: Oid4vciController;

  beforeEach(() => {
    oid4vci = {
      createOffer: jest.fn(),
      getOffer: jest.fn(),
      token: jest.fn(),
      issueNonce: jest.fn(),
      credential: jest.fn(),
      deferred: jest.fn(),
      notification: jest.fn(),
    };
    controller = new Oid4vciController(oid4vci);
  });

  describe('createOffer', () => {
    it('delegates to oid4vci.createOffer with the body', () => {
      const body = { credentialSchemaId: 'schema-1' };
      const expected = { qr_data: 'openid-credential-offer://' };
      oid4vci.createOffer.mockReturnValue(expected);

      const result = controller.createOffer(body);
      expect(result).toBe(expected);
      expect(oid4vci.createOffer).toHaveBeenCalledWith(body);
    });
  });

  describe('getOffer', () => {
    it('delegates to oid4vci.getOffer with the id', () => {
      const expected = { credential_issuer: 'https://issuer.example' };
      oid4vci.getOffer.mockReturnValue(expected);

      const result = controller.getOffer('offer-1');
      expect(result).toBe(expected);
      expect(oid4vci.getOffer).toHaveBeenCalledWith('offer-1');
    });
  });

  describe('token', () => {
    it('delegates to oid4vci.token with the body', () => {
      const body = { grant_type: 'urn:ietf:params:oauth:grant-type:pre-authorized_code' };
      const expected = { access_token: 'abc' };
      oid4vci.token.mockReturnValue(expected);

      const result = controller.token(body);
      expect(result).toBe(expected);
      expect(oid4vci.token).toHaveBeenCalledWith(body);
    });

    it('defaults a null/undefined body to {}', () => {
      controller.token(undefined);
      expect(oid4vci.token).toHaveBeenCalledWith({});
    });
  });

  describe('nonce', () => {
    it('wraps oid4vci.issueNonce() result as { c_nonce }', async () => {
      oid4vci.issueNonce.mockResolvedValue('nonce-value');

      const result = await controller.nonce();
      expect(result).toEqual({ c_nonce: 'nonce-value' });
      expect(oid4vci.issueNonce).toHaveBeenCalled();
    });
  });

  describe('credential', () => {
    it('delegates to oid4vci.credential with auth header and body', () => {
      const body = { format: 'vc+sd-jwt' };
      const expected = { credential: 'signed-jwt' };
      oid4vci.credential.mockReturnValue(expected);

      const result = controller.credential('Bearer abc', body);
      expect(result).toBe(expected);
      expect(oid4vci.credential).toHaveBeenCalledWith('Bearer abc', body);
    });

    it('defaults a null/undefined body to {}', () => {
      controller.credential('Bearer abc', undefined);
      expect(oid4vci.credential).toHaveBeenCalledWith('Bearer abc', {});
    });
  });

  describe('deferred', () => {
    it('calls res.status(202) when the result is pending', async () => {
      const res = { status: jest.fn() };
      const pendingResult = { pending: true };
      oid4vci.deferred.mockResolvedValue(pendingResult);

      const result = await controller.deferred('Bearer abc', { transaction_id: 't1' }, res as any);
      expect(result).toBe(pendingResult);
      expect(oid4vci.deferred).toHaveBeenCalledWith('Bearer abc', { transaction_id: 't1' });
      expect(res.status).toHaveBeenCalledWith(202);
    });

    it('does not call res.status when the result is not pending', async () => {
      const res = { status: jest.fn() };
      const finalResult = { credential: 'signed-jwt' };
      oid4vci.deferred.mockResolvedValue(finalResult);

      const result = await controller.deferred('Bearer abc', undefined, res as any);
      expect(result).toBe(finalResult);
      expect(oid4vci.deferred).toHaveBeenCalledWith('Bearer abc', {});
      expect(res.status).not.toHaveBeenCalled();
    });
  });

  describe('notification', () => {
    it('delegates to oid4vci.notification with auth header and body', () => {
      const body = { event: 'credential_accepted' };
      controller.notification('Bearer abc', body);
      expect(oid4vci.notification).toHaveBeenCalledWith('Bearer abc', body);
    });

    it('defaults a null/undefined body to {}', () => {
      controller.notification('Bearer abc', undefined);
      expect(oid4vci.notification).toHaveBeenCalledWith('Bearer abc', {});
    });
  });
});