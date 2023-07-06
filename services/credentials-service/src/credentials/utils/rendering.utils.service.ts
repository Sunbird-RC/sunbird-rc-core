import {
  Injectable,
  InternalServerErrorException,
  Logger,
} from "@nestjs/common";
import { W3CCredential } from "did-jwt-vc";
import { JwtCredentialSubject } from "src/app.interface";
import * as wkhtmltopdf from "wkhtmltopdf";
import { compile } from "handlebars";
import { HttpService } from "@nestjs/axios";
// eslint-disable-next-line @typescript-eslint/no-var-requires
const QRCode = require("qrcode");

@Injectable()
export class RenderingUtilsService {
  async generateQR(cred: W3CCredential) {
    try {
      const verificationURL = `${process.env.CREDENTIAL_SERVICE_BASE_URL}/credentials/${cred.id}/verify`;
      const QRData = await QRCode.toDataURL(verificationURL);
      return QRData;
    } catch (err) {
      Logger.error("Error rendering QR: ", err);
      return err;
    }
  }

  async compileHBSTemplate(
    credential: W3CCredential,
    template: string
  ): Promise<string> {
    try {
      const subject: JwtCredentialSubject = credential.credentialSubject;
      subject["qr"] = await this.generateQR(credential);
      const hbsTemplate = compile(template);
      return hbsTemplate(subject);
    } catch (err) {
      Logger.error("Error compiling HBS template: ", err);
      throw new InternalServerErrorException("Error compiling HBS template");
    }
  }

  async renderAsPDF(credential: W3CCredential, template: string) {
    try {
      const data = await this.compileHBSTemplate(credential, template);
      return wkhtmltopdf(data, {
        pageSize: "A4",
        disableExternalLinks: true,
        disableInternalLinks: true,
        disableJavascript: true,
        encoding: "UTF-8",
      });
    } catch (err) {
      Logger.error("Error rendering PDF: ", err);
      throw new InternalServerErrorException("Error rendering PDF");
    }
  }
}
