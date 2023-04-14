const CUSTOM_TEMPLATE_DELIMITERS = process.env.CUSTOM_TEMPLATE_DELIMITERS?.split(',') || "{{,}}".split(",")
const CERTIFICATE_ORIENTATION = process.env.CERTIFICATE_ORIENTATION.toLowerCase() === "landscape" ? "landscape" : "portrait"
const CERTIFICATE_PAGE_SIZE = process.env.CERTIFICATE_PAGE_SIZE || 'A4';
module.exports = {
    CUSTOM_TEMPLATE_DELIMITERS,
    CERTIFICATE_ORIENTATION,
    CERTIFICATE_PAGE_SIZE
};
