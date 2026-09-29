import { RenderingUtilsService } from './rendering.utils.service';

// Mock external deps
jest.mock('qrcode', () => ({ toDataURL: jest.fn().mockResolvedValue('data:image/png;base64,abc123') }));
jest.mock('@mosip/pixelpass', () => ({ generateQRCode: jest.fn().mockReturnValue('data:image/png;base64,qrdata') }));
jest.mock('handlebars', () => ({
  compile: jest.fn().mockReturnValue((data: any) => `<html><body>${JSON.stringify(data)}</body></html>`),
}));
jest.mock('wkhtmltopdf', () => jest.fn().mockReturnValue(Buffer.from('pdf-content')));

describe('RenderingUtilsService', () => {
  let service: RenderingUtilsService;

  beforeEach(() => {
    service = new RenderingUtilsService();
    process.env.CREDENTIAL_SERVICE_BASE_URL = 'http://localhost:3000';
  });

  describe('generateQR', () => {
    it('returns QR data URL with verify URL (default QR_TYPE)', async () => {
      const cred = { id: 'urn:uuid:test-cred' } as any;
      const result = await service.generateQR(cred);
      expect(result).toBeDefined();
      expect(result).toContain('data:image/png;base64,');
    });

    it('returns QR data URL with credential JSON when QR_TYPE=W3C_VC', async () => {
      process.env.QR_TYPE = 'W3C_VC';
      const cred = { id: 'urn:uuid:test-cred', type: ['VerifiableCredential'] } as any;
      const result = await service.generateQR(cred);
      expect(result).toBeDefined();
      delete process.env.QR_TYPE;
    });

    it('throws InternalServerErrorException on error', async () => {
      const { generateQRCode } = require('@mosip/pixelpass');
      generateQRCode.mockImplementationOnce(() => { throw new Error('qr fail'); });
      await expect(service.generateQR({ id: 'x' } as any)).rejects.toThrow('Error rendering QR');
    });
  });

  describe('compileHBSTemplate', () => {
    it('renders credential subject into template', async () => {
      const credential = {
        id: 'urn:uuid:test',
        credentialSubject: { id: 'did:test:1', grade: 'A', programme: 'CS' },
      } as any;
      const result = await service.compileHBSTemplate(credential, '<html>{{grade}}</html>');
      expect(result).toContain('A');
      expect(result).toContain('CS');
    });

    it('injects QR into credential subject', async () => {
      const credential = {
        id: 'urn:uuid:test',
        credentialSubject: { id: 'did:test:1' },
      } as any;
      await service.compileHBSTemplate(credential, '<html></html>');
      expect((credential.credentialSubject as any).qr).toBeDefined();
    });

    it('throws InternalServerErrorException on template error', async () => {
      const { compile } = require('handlebars');
      compile.mockImplementationOnce(() => { throw new Error('bad template'); });
      await expect(
        service.compileHBSTemplate({ id: 'x', credentialSubject: {} } as any, 'bad')
      ).rejects.toThrow('Error compiling HBS template');
    });
  });

  describe('renderAsPDF', () => {
    it('returns a buffer from wkhtmltopdf', async () => {
      const credential = {
        id: 'urn:uuid:test',
        credentialSubject: { id: 'did:test:1', grade: 'A' },
      } as any;
      const result = await service.renderAsPDF(credential, '<html>{{grade}}</html>');
      expect(result).toBeDefined();
      expect(Buffer.isBuffer(result)).toBe(true);
    });
  });
});
