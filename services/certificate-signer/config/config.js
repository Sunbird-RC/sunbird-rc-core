const CERTIFICATE_CONTROLLER_ID = process.env.CERTIFICATE_CONTROLLER_ID || 'https://sunbird.org/';
const CERTIFICATE_ISSUER = process.env.CERTIFICATE_ISSUER || "https://sunbird.org/";
const CUSTOM_TEMPLATE_DELIMITERS = process.env.CUSTOM_TEMPLATE_DELIMITERS?.split(',') || "{{,}}".split(",");
const CACHE_CONTEXT_URLS = process.env.CACHE_CONTEXT_URLS || "";
const TIME_ZONE = process.env.TIME_ZONE;
module.exports = {
  CERTIFICATE_CONTROLLER_ID,
  CERTIFICATE_ISSUER,
  CUSTOM_TEMPLATE_DELIMITERS,
  CACHE_CONTEXT_URLS,
  TIME_ZONE
};
