import { InternalServerErrorException, Logger } from "@nestjs/common";
import { W3CCredential } from "did-jwt-vc";
import { JwtCredentialSubject } from "src/app.interface";
import * as wkhtmltopdf from 'wkhtmltopdf';
import { compile } from "handlebars";
// eslint-disable-next-line @typescript-eslint/no-var-requires
const QRCode = require('qrcode');


export const generateQR = async (cred: W3CCredential) => {
  try {
    const verificationURL = `${process.env.CREDENTIAL_SERVICE_BASE_URL}/credentials/${cred.id}/verify`;
    const QRData = await QRCode.toDataURL(verificationURL);
    return QRData;
  } catch (err) {
    Logger.error('Error rendering QR: ', err);
    return err;
  }
}

export const compileHBSTemplate = async (credential: W3CCredential, template: string): Promise<string> => {
  try {
    const subject: JwtCredentialSubject = credential.credentialSubject;
    subject['qr'] = await generateQR(credential);
    const hbsTemplate = compile(template);
    return hbsTemplate(subject);
  } catch (err) {
    Logger.error('Error compiling HBS template: ', err);
    throw new InternalServerErrorException('Error compiling HBS template');
  }
}

export const renderAsPDF = async (credential: W3CCredential, template: string) => {
  try {
    const data = await compileHBSTemplate(credential, template);
    return wkhtmltopdf(data, {
      pageSize: 'A4',
      disableExternalLinks: true,
      disableInternalLinks: true,
      disableJavascript: true,
      encoding: 'UTF-8',
    });
  } catch (err) {
    Logger.error('Error rendering PDF: ', err);
    throw new InternalServerErrorException('Error rendering PDF');
  }
}

