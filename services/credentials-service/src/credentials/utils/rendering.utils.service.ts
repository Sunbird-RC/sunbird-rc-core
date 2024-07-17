import {
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';
import { W3CCredential } from 'vc.types';
import { JwtCredentialSubject } from 'src/app.interface';
import * as wkhtmltopdf from 'wkhtmltopdf';
import { compile } from 'handlebars';
import * as QRCode from 'qrcode';
import JSZip from 'jszip';

@Injectable()
export class RenderingUtilsService {
  private logger = new Logger(RenderingUtilsService.name);

  async generateQR(cred: W3CCredential) {
    try {
      if(!QRCode) {
        console.log("library QRCode is not loaded!");
      }
      let qrData = `${process.env.CREDENTIAL_SERVICE_BASE_URL}/credentials/${cred.id}/verify`;
      if(process?.env?.QR_TYPE === "W3C_VC") {
        const zip = new JSZip();
        zip.file("certificate.json", JSON.stringify(cred), {
          compression: "DEFLATE"
        });
        qrData = await zip.generateAsync({type: 'string', compression: "DEFLATE"})
          .then(function (content) {
            return content;
          });
        return QRCode.toDataURL(qrData, {scale: 3});
      }
      return QRCode.toDataURL(qrData);
    } catch (err) {
      console.log(err);
      this.logger.error('Error rendering QR: ', err);
      throw new InternalServerErrorException('Error rendering QR');
    }
  }

  async compileHBSTemplate(
    credential: W3CCredential,
    template: string
  ): Promise<string> {
    try {
      const subject: JwtCredentialSubject = credential.credentialSubject;
      subject['qr'] = await this.generateQR(credential);
      const hbsTemplate = compile(template);
      return hbsTemplate(subject);
    } catch (err) {
      this.logger.error('Error compiling HBS template: ', err);
      throw new InternalServerErrorException('Error compiling HBS template');
    }
  }

  async renderAsPDF(credential: W3CCredential, template: string) {
    try {
      const data = await this.compileHBSTemplate(credential, template);
      return wkhtmltopdf(data, {
        pageSize: 'A4',
        disableExternalLinks: true,
        disableInternalLinks: true,
        disableJavascript: true,
        encoding: 'UTF-8',
      });
    } catch (err) {
      console.log(err);
      this.logger.error('Error rendering PDF: ', err);
      throw new InternalServerErrorException('Error rendering PDF');
    }
  }
}
