const CERTIFICATE_NAMESPACE = process.env.CERTIFICATE_NAMESPACE || "https://sunbird.org/credentials/vaccination/v1";
const CERTIFICATE_CONTROLLER_ID = process.env.CERTIFICATE_CONTROLLER_ID || 'https://sunbird.org/';
const CERTIFICATE_PUBKEY_ID = process.env.CERTIFICATE_PUBKEY_ID || 'https://example.com/i/india';
const CERTIFICATE_DID = process.env.CERTIFICATE_DID || 'did:india';
const CERTIFICATE_ISSUER = process.env.CERTIFICATE_ISSUER || "https://sunbird.org/";
const CUSTOM_TEMPLATE_DELIMITERS = process.env.CUSTOM_TEMPLATE_DELIMITERS?.split(',') || "{{,}}".split(",")

module.exports = {
  CERTIFICATE_NAMESPACE,
  CERTIFICATE_CONTROLLER_ID,
  CERTIFICATE_DID,
  CERTIFICATE_PUBKEY_ID,
  CERTIFICATE_ISSUER,
  CUSTOM_TEMPLATE_DELIMITERS
};
