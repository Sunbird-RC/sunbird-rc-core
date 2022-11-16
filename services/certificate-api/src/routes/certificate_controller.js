const fs = require('fs');
var url = require('url');
const Handlebars = require('handlebars');
const puppeteer = require('puppeteer');
const QRCode = require('qrcode');
const JSZip = require("jszip");
const { default: axios } = require('axios');
const URL_W3C_VC = 'URL-W3C-VC';
const URL = 'URL';
const envData = require('../../configs/keys');
const {CUSTOM_TEMPLATE_DELIMITERS} = require('../../configs/config');
const delimiters = require('handlebars-delimiters');
const NodeCache = require("node-cache");
const hash = require('object-hash');

const cacheInstance = new NodeCache();

Handlebars.registerHelper('dateFormat', require('handlebars-dateformat'));
const browserConfig = {
    headless: true,
    //comment to use default
    executablePath: '/usr/bin/chromium-browser',
    args: [
        "--no-sandbox",
        "--disable-gpu",
    ]
}
delimiters(Handlebars, CUSTOM_TEMPLATE_DELIMITERS);
let browser;
(async function () {
    browser = await puppeteer.launch(browserConfig);
})();

function getNumberWithOrdinal(n) {
    const s = ["th", "st", "nd", "rd"],
        v = n % 100;
    return n + " " + (s[(v - 20) % 10] || s[v] || s[0]);
}

function appendCommaIfNotEmpty(address, suffix) {
    if (address.trim().length > 0) {
        if (suffix.trim().length > 0) {
            return address + ", " + suffix
        } else {
            return address
        }
    }
    return suffix
}

function concatenateReadableString(a, b) {
    let address = "";
    address = appendCommaIfNotEmpty(address, a);
    address = appendCommaIfNotEmpty(address, b);
    if (address.length > 0) {
        return address
    }
    return "NA"
}

const monthNames = [
    "Jan", "Feb", "Mar", "Apr",
    "May", "Jun", "Jul", "Aug",
    "Sep", "Oct", "Nov", "Dec"
];

function formatDate(givenDate) {
    const dob = new Date(givenDate);
    let day = dob.getDate();
    let monthName = monthNames[dob.getMonth()];
    let year = dob.getFullYear();

    return `${padDigit(day)}-${monthName}-${year}`;
}

function month(givenDateTime){
    const dob = new Date(givenDateTime);
    let monthName = monthNames[dob.getMonth()];
    return `${monthName}`
}

function day(givenDateTime){
    const dob = new Date(givenDateTime);
    let day = dob.getDate();
    return `${day}`
}

function year(givenDateTime){
    const dob = new Date(givenDateTime);
    let year = dob.getFullYear();
    return `${year}`
}

function formatDateTime(givenDateTime) {
    const dob = new Date(givenDateTime);
    let day = dob.getDate();
    let monthName = monthNames[dob.getMonth()];
    let year = dob.getFullYear();
    let hour = dob.getHours();
    let minutes = dob.getMinutes();

    return `${padDigit(day)}-${monthName}-${year} ${hour}:${minutes}`;

}

function padDigit(digit, totalDigits = 2) {
    return String(digit).padStart(totalDigits, '0')
}

const getRequestBody = async (req) => {
    const buffers = []
    for await (const chunk of req) {
        buffers.push(chunk)
    }

    const data = Buffer.concat(buffers).toString();
    if (data === "") return undefined;
    return JSON.parse(data);
};

async function generateRawCertificate(certificate, templateUrl, entityId, entityName) {
    let certificateRaw = certificate;
    // TODO: based on type template will be picked
    const certificateTemplateUrl = templateUrl;
    const qrCodeType = envData.qrType || '';
    let qrData;
    console.log('QR Code type: ', qrCodeType);
    if (qrCodeType.toUpperCase() === URL) {
        qrData = `${envData.certDomainUrl}/certs/${entityId}?t=${qrCodeType}&entity=${entityName}${process.env.ADDITIONAL_QUERY_PARAMS || ""}`;
    } else {
        const zip = new JSZip();
        zip.file("certificate.json", certificateRaw, {
            compression: "DEFLATE"
        });
        const zipType = (qrCodeType && qrCodeType.toUpperCase() === URL_W3C_VC);
        const zippedData = await zip.generateAsync({type: zipType ? 'base64': 'string', compression: "DEFLATE"})
            .then(function (content) {
                return content;
            });
        qrData = zippedData
        if (zipType) {
            console.log('ZippedData length', String(zippedData).length);
            qrData = `${envData.certDomainUrl}/certs/${entityId}?t=${envData.qrType}&data=${zippedData}&entity=${entityName}${process.env.ADDITIONAL_QUERY_PARAMS || ""}`;
        }
    }
    
    const dataURL = await QRCode.toDataURL(qrData, {scale: 3});  
    const certificateData = prepareDataForCertificateWithQRCode(certificateRaw, dataURL);
    return await renderDataToTemplate(certificateTemplateUrl, certificateData);
}

async function createCertificatePDF(certificate, templateUrl, res, entityId, entityName) {
    let rawCertificate = await generateRawCertificate(certificate, templateUrl, entityId, entityName);
    const pdfBuffer = await createPDF(rawCertificate);
    res.statusCode = 200;
    return pdfBuffer;
}
function isEmpty(obj) {
    return Object.keys(obj).length === 0;
}

function sendResponse(res, statusCode, message) {
    res.statusCode=statusCode;
    res.end(message);
}

async function getCertificatePDF(req, res) {
    try {
        const reqBody = await getRequestBody(req)
        if (!reqBody || isEmpty(reqBody)) {
            return sendResponse(res, 400, "Bad request");
        }
        console.log('Got this req', reqBody);
        let {certificate, templateUrl, entityId, entityName} = reqBody;
        if (certificate === "" || templateUrl === "") {
            return sendResponse(res, 400, "Required parameters missing");
        }
        res = await createCertificatePDF(certificate, templateUrl, res, entityId, entityName);
        return res
    } catch (err) {
        console.error(err);
        res.statusCode = 500;
    }
}

async function getCertificate(req, res) {
    try {
        const reqBody = await getRequestBody(req)
        if (!reqBody || isEmpty(reqBody)) {
            return sendResponse(res, 400, "Bad request");
        }
        console.log('Got this req', reqBody);
        let {certificate, templateUrl, entityId, entityName} = reqBody;
        if (certificate === "" || templateUrl === "") {
            return sendResponse(res, 400, "Required parameters missing");
        }
        res = await generateRawCertificate(certificate, templateUrl, entityId, entityName);
        return res
    } catch (err) {
        console.error(err);
        res.statusCode = 500;
    }
}

const fetchCachedTemplate = async (templateFileURL) => {
    console.log("Fetching credential templates: ", templateFileURL);
    const template = cacheInstance.get(templateFileURL);
    if (template === undefined) {
        let template = await axios.get(templateFileURL).then(res => res.data);
        cacheInstance.set(templateFileURL, template);
        console.debug("Fetched credential templates from API");
        return template;
    } else {
        console.debug("Fetched credential templates from cache");
        return template;
    }
};

async function getTemplate(templateFileURL) {
    return await fetchCachedTemplate(templateFileURL);
}

const getHandleBarTemplate = (credentialTemplate) => {
    const credentialTemplateHash = hash(credentialTemplate);
    if (cacheInstance.has(credentialTemplateHash)) {
        console.debug("Credential template loaded from cache");
        return cacheInstance.get(credentialTemplateHash);
    } else {
        let handleBarTemplate = Handlebars.compile(credentialTemplate);
        cacheInstance.set(credentialTemplateHash, handleBarTemplate);
        console.debug("Credential template stored in cache");
        return handleBarTemplate;
    }
};

async function renderDataToTemplate(templateFileURL, data) {
    console.log("rendering data to template")
    // const htmlData = fs.readFileSync(templateFileURL, 'utf8');
    const htmlData = await getTemplate(templateFileURL);
    // console.log('Received ', htmlData);
    const template = getHandleBarTemplate(htmlData);
    return template(data);
}

async function createPDF(certificate) {
    try {
        if (!browser) {
            browser = await puppeteer.launch(browserConfig);
        }
        const page = await browser.newPage();
        await page.evaluateHandle('document.fonts.ready');
        await page.setContent(certificate, {
            waitUntil: 'domcontentloaded'
        });
        // console.log(certificate);
        // await page.goto('data:text/html,' + certificate, {waitUntil: 'networkidle2'});
        await page.evaluateHandle('document.fonts.ready');
        const pdfBuffer = await page.pdf({
            format: 'A4',
            printBackground: true,
            displayHeaderFooter: true
        });


        // close the browser
        await page.close()
        return pdfBuffer
    } catch (e) {
        console.log("Failed while creating pdf")
        console.log(e)
    }
}

function prepareDataForCertificateWithQRCode(certificateRaw, dataURL) {
    console.log("Preparing data for certificate template")
    certificateRaw = JSON.parse(certificateRaw);
    const certificateData = {
        ...certificateRaw,
        qrCode: dataURL
    };

    return certificateData;
}

module.exports = {
    getCertificatePDF,
    getCertificate
};
