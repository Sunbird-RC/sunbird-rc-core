export const publicKeyPem = process.env.CERTIFICATE_PUBLIC_KEY || '';
// eslint-disable-next-line max-len
export const privateKeyPem = process.env.CERTIFICATE_PRIVATE_KEY || '';
export const qrType = process.env.QR_TYPE || 'W3C-VC';
export const certDomainUrl = process.env.CERTIFICATE_DOMAIN_URL || "https://dev.sunbirded.org";
export const smsAuthKey = "";

export default {
  publicKeyPem, privateKeyPem,
  qrType,
  certDomainUrl,
  smsAuthKey
}

/*
// openssl genrsa -out key.pem; cat key.pem;
// openssl rsa -in key.pem -pubout -out pubkey.pem;
// cat pubkey.pem; rm key.pem pubkey.pem
*/