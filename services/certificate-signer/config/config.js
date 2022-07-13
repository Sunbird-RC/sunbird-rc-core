const CERTIFICATE_CONTROLLER_ID = process.env.CERTIFICATE_CONTROLLER_ID || 'https://sunbird.org/';
const CERTIFICATE_PUBKEY_ID = process.env.CERTIFICATE_PUBKEY_ID || 'https://example.com/i/india';
const CERTIFICATE_DID = process.env.CERTIFICATE_DID || 'did:authorizedSigner:123456789';
const CERTIFICATE_ISSUER = process.env.CERTIFICATE_ISSUER || "https://sunbird.org/";
const CUSTOM_TEMPLATE_DELIMITERS = process.env.CUSTOM_TEMPLATE_DELIMITERS?.split(',') || "{{,}}".split(",");
const CACHE_CONTEXT_URLS = process.env.CACHE_CONTEXT_URLS || "";

module.exports = {
  CERTIFICATE_CONTROLLER_ID,
  CERTIFICATE_DID,
  CERTIFICATE_PUBKEY_ID,
  CERTIFICATE_ISSUER,
  CUSTOM_TEMPLATE_DELIMITERS,
  CACHE_CONTEXT_URLS
};
