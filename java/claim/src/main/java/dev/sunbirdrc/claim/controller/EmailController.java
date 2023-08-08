package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.dto.BarCode;
import dev.sunbirdrc.claim.dto.MailDto;
import dev.sunbirdrc.claim.service.EmailService;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;


@Controller
public class EmailController {

    @Autowired
    private EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    private static final Font BARCODE_TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
    @Autowired
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @RequestMapping(value = "/api/v1/sendMail", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> sendMail(@RequestHeader HttpHeaders headers,
                                                @RequestBody MailDto requestBody) {
        String email = requestBody.getEmailAddress();
        String idLink = requestBody.getCertificate();
        String name = requestBody.getName();
        String credType = requestBody.getCredentialsType();
        String body = prepareBody(idLink, name, credType);
        emailService.sendMail(email, credType + " for Student", body);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/barcode", method = RequestMethod.POST)
    public ResponseEntity<BarCode> getBarCode(@RequestHeader HttpHeaders headers,
                                                        @RequestBody BarCode requestBody) {
        BarCode code = new BarCode();
        String barCode = requestBody.getBarCodeText();
        logger.info("In Controller::"+barCode);
        String text  = null;
        try {
            text  = generateBarcodeImage(barCode);
            code.setBarCodeText(barCode);
            code.setBarCodeValue(text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ResponseEntity<>(code, HttpStatus.OK);
    }

    public static String generateBarcodeImage(String barcodeText) throws Exception {
        Barcode barcode = BarcodeFactory.createCode128(barcodeText);
        logger.info(barcodeText);
        barcode.setFont(BARCODE_TEXT_FONT);
        barcode.setResolution(200);
        BufferedImage image = BarcodeImageHandler.getImage(barcode);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        String data = DatatypeConverter.printBase64Binary(baos.toByteArray());
        data = data.replaceAll("&amp;","\'&\'");
        data = data.replaceAll("&","\'&\'");
        String imageString = "data:image/png;base64," + data;

       // String html = "<img src='" + imageString + "'>";
        //html = Pattern.quote(html);
        logger.info(imageString);
        return imageString;
    }
    private String prepareBody(String idLink, String name, String credType) {

        String body = "Hi "+ name + ","+
                "\n" +
                " \n" +
                "\n" +
                "We are pleased to inform you that a " +credType
                +
                " has been issued to you. You can view and download the credential by using the following link. \n" +
                "\n" +
                "\n" +
                idLink +
                " \n" +
                "\n" +
                "Thank you, \n" +
                "\n" +
                "<Registration Credential Issuing Authority> ";
        return body;
    }


}
