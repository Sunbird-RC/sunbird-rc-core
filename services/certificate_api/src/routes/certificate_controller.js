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

function formatRecipientAddress(address) {
    return concatenateReadableString(address.streetAddress, address.district)
}

function formatFacilityAddress(evidence) {
    return concatenateReadableString(evidence.facility.name, evidence.facility.address.district)
}

function formatId(identity) {
    const split = identity.split(":");
    const lastFragment = split[split.length - 1];
    if (identity.includes("aadhaar") && lastFragment.length >= 4) {
        return "Aadhaar # XXXX XXXX XXXX " + lastFragment.substr(lastFragment.length - 4)
    }
    if (identity.includes("Driving")) {
        return "Driver’s License # " + lastFragment
    }
    if (identity.includes("MNREGA")) {
        return "MNREGA Job Card # " + lastFragment
    }
    if (identity.includes("PAN")) {
        return "PAN Card # " + lastFragment
    }
    if (identity.includes("Passbooks")) {
        return "Passbook # " + lastFragment
    }
    if (identity.includes("Passport")) {
        return "Passport # " + lastFragment
    }
    if (identity.includes("Pension")) {
        return "Pension Document # " + lastFragment
    }
    if (identity.includes("Voter")) {
        return "Voter ID # " + lastFragment
    }
    return lastFragment
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

async function createCertificatePDF(certificateInputReq, res) {
    let certificateRaw = certificateInputReq.data;
    // TODO: based on type template will be picked
    const certificateTemplateUrl = certificateInputReq.templateUrl;
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
    const certificateData = prepareDataForVaccineCertificateTemplate(certificateRaw, dataURL);
    const pdfBuffer = await createPDF(certificateTemplateUrl, certificateData);
    res.statusCode = 200;
    return pdfBuffer;
}

async function getCertificatePDF(req, res) {
    try {
        const buffers = []
        for await (const chunk of req) {
            buffers.push(chunk)
        }

        const data = Buffer.concat(buffers).toString();
        const reqBody = JSON.parse(data);
        console.log('Got this req', reqBody);
        
        res = await createCertificatePDF(reqBody, res);
        return res
    } catch (err) {
        console.error(err);
        res.statusCode = 404;
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
        // executablePath: '/usr/bin/chromium-browser',
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

function prepareDataForVaccineCertificateTemplate(certificateRaw, dataURL) {
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
