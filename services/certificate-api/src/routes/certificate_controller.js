const fs = require('fs');
var url = require('url');
const Handlebars = require('handlebars');
const puppeteer = require('puppeteer');
const QRCode = require('qrcode');
const JSZip = require("jszip");
const { default: axios } = require('axios');

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

async function createCertificatePDF(certificate, templateUrl, res) {
    let certificateRaw = certificate;
    // TODO: based on type template will be picked
    const certificateTemplateUrl = templateUrl;
    const zip = new JSZip();
    zip.file("certificate.json", certificateRaw, {
        compression: "DEFLATE"
    });
    const zippedData = await zip.generateAsync({type: "string", compression: "DEFLATE"})
        .then(function (content) {
            // console.log(content)
            return content;
        });

    const dataURL = await QRCode.toDataURL(zippedData, {scale: 2});
    const certificateData = prepareDataForCertificateWithQRCode(certificateRaw, dataURL);
    const pdfBuffer = await createPDF(certificateTemplateUrl, certificateData);
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
        let {certificate, templateUrl} = reqBody;
        if (certificate === "" || templateUrl === "") {
            return sendResponse(res, 400, "Required parameters missing");
        }
        res = await createCertificatePDF(certificate, templateUrl, res);
        return res
    } catch (err) {
        console.error(err);
        res.statusCode = 500;
    }
}

async function getTemplate(templateFileURL) {
    const templateContent = await axios.get(templateFileURL).then(res => res.data);
    return templateContent;
}

async function createPDF(templateFileURL, data) {
    console.log("Creating pdf")
    // const htmlData = fs.readFileSync(templateFileURL, 'utf8');
    const htmlData = await getTemplate(templateFileURL);
    console.log('Received ', htmlData);
    const template = Handlebars.compile(htmlData);
    let certificate = template(data);
    const browser = await puppeteer.launch({
        headless: true,
        //comment to use default
        executablePath: '/usr/bin/chromium-browser',
        args: [
            "--no-sandbox",
            "--disable-gpu",
        ]
    });
    const page = await browser.newPage();
    await page.evaluateHandle('document.fonts.ready');
    await page.setContent(certificate, {
        waitUntil: 'domcontentloaded'
    });
    const pdfBuffer = await page.pdf({
        format: 'A4',
        printBackground: true,
        displayHeaderFooter: true
    });

    // close the browser
    await browser.close();

    return pdfBuffer
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
    getCertificatePDF
};
