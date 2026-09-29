import { WellKnownController } from './well-known.controller';
import { JwtSignerService } from '../vc/jwt.service';

describe('WellKnownController', () => {
  let controller: WellKnownController;
  let jwtSigner: { getJwks: jest.Mock };

  beforeEach(() => {
    jwtSigner = { getJwks: jest.fn() };
    controller = new WellKnownController(jwtSigner as unknown as JwtSignerService);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('delegates to and returns JwtSignerService.getJwks()', async () => {
    const jwks = { keys: [{ kty: 'EC', kid: 'did:rcw:1#jwt-key-1' }] };
    jwtSigner.getJwks.mockResolvedValue(jwks);

    const result = await controller.getJwks();
    expect(result).toEqual(jwks);
    expect(jwtSigner.getJwks).toHaveBeenCalled();
  });

  it('propagates a rejection from JwtSignerService.getJwks()', async () => {
    jwtSigner.getJwks.mockRejectedValue(new Error('boom'));
    await expect(controller.getJwks()).rejects.toThrow('boom');
  });
});