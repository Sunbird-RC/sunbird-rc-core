const CUSTOM_TEMPLATE_DELIMITERS = process.env.CUSTOM_TEMPLATE_DELIMITERS?.split(',') || "{{,}}".split(",")
module.exports = {
    CUSTOM_TEMPLATE_DELIMITERS
};
