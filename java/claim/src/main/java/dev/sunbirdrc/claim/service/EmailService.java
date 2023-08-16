package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.config.PropertyMapper;
import dev.sunbirdrc.claim.controller.EmailController;
import dev.sunbirdrc.claim.dto.CertificateMailDto;
import dev.sunbirdrc.claim.dto.MailDto;
import dev.sunbirdrc.claim.dto.PendingMailDTO;
import dev.sunbirdrc.claim.exception.ClaimMailException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service("emailService")
public class EmailService 
{
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    @Autowired
    private JavaMailSender mailSender;
     
    @Autowired
    private SimpleMailMessage preConfiguredMessage;

    @Autowired
    private Configuration freeMarkerConfiguration;

    @Value("${simple.mail.message.from}")
    private String simpleMailMessageFrom;

    @Autowired
    private PropertyMapper propertyMapper;
 
    /**
     * This method will send compose and send the message 
     * */
    public void sendMail(String to, String subject, String body) 
    {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom("shishir.suman@tarento.com");
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
 
    /**
     * This method will send a pre-configured message
     * */
    public void sendPreConfiguredMail(String message) 
    {
        SimpleMailMessage mailMessage = new SimpleMailMessage(preConfiguredMessage);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }
    
    public void sendMailWithAttachment(String to, String subject, String body, String fileToAttach) 
    {
    	MimeMessagePreparator preparator = new MimeMessagePreparator() 
    	{
            public void prepare(MimeMessage mimeMessage) throws Exception 
            {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                mimeMessage.setFrom(new InternetAddress("admin@gmail.com"));
                mimeMessage.setSubject(subject);
                mimeMessage.setText(body);
                
                FileSystemResource file = new FileSystemResource(new File(fileToAttach));
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.addAttachment("logo.jpg", file);
            }
        };
        
        try {
            mailSender.send(preparator);
        }
        catch (MailException ex) {
            // simply log it and go on...
            System.err.println(ex.getMessage());
        }
    }
    
    public void sendMailWithInlineResources(String to, String subject, String fileToAttach) 
    {
    	MimeMessagePreparator preparator = new MimeMessagePreparator() 
    	{
            public void prepare(MimeMessage mimeMessage) throws Exception 
            {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                mimeMessage.setFrom(new InternetAddress("admin@gmail.com"));
                mimeMessage.setSubject(subject);
                
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                
                helper.setText("<html><body><img src='cid:identifier1234'></body></html>", true);
                
                FileSystemResource res = new FileSystemResource(new File(fileToAttach));
                helper.addInline("identifier1234", res);
            }
        };
        
        try {
            mailSender.send(preparator);
        }
        catch (MailException ex) {
            // simply log it and go on...
            System.err.println(ex.getMessage());
        }
    }

    @Async
    public void sendCertificateMail(CertificateMailDto certificateMailDto) {
        String subject = certificateMailDto.getCredentialsType();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setFrom(new InternetAddress(propertyMapper.getSimpleMailMessageFrom(), certificateMailDto.getName()));
            mimeMessageHelper.setTo(certificateMailDto.getEmailAddress());
            mimeMessageHelper.setText(generateAttachedCertificateMailContent(certificateMailDto), true);


            byte[] doc = Base64.getDecoder().decode(certificateMailDto.getCertificateBase64());
            mimeMessageHelper.addAttachment("certificate.pdf", new ByteArrayResource(doc));

            mailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (Exception e) {
            logger.error("Exception while sending mail: ", e);
            throw new ClaimMailException("Exception while composing and sending mail with OTP");
        }
    }

    private String generateAttachedCertificateMailContent(CertificateMailDto certificateMailDto) {
        String processedTemplateString = null;

        Map<String, Object> mailMap = new HashMap<>();
        mailMap.put("name", certificateMailDto.getName());
        mailMap.put("credType", certificateMailDto.getCredentialsType());
        mailMap.put("idLink", certificateMailDto.getCertificate());

        try {
            freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/templates/");
            Template template = freeMarkerConfiguration.getTemplate("credentials-mail.ftl");
            processedTemplateString = FreeMarkerTemplateUtils.processTemplateIntoString(template, mailMap);

        } catch (TemplateException e) {
            logger.error("TemplateException while creating mail template for certificate ", e);
            throw new ClaimMailException("Error while creating mail template for certificate");
        } catch (IOException e) {
            logger.error("IOException while creating mail template for certificate ", e);
            throw new ClaimMailException("Error while creating mail template for certificate");
        }
        return processedTemplateString;
    }

    @Async
    public void sendPendingMail(PendingMailDTO pendingMailDTO) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setSubject(propertyMapper.getForeignPendingItemSubject());
            mimeMessageHelper.setFrom(new InternetAddress(propertyMapper.getSimpleMailMessageFrom(),pendingMailDTO.getName()));
            mimeMessageHelper.setTo(pendingMailDTO.getEmailAddress());
            mimeMessageHelper.setText(generateCertificateMailContent(pendingMailDTO), true);

            mailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (Exception e) {
            logger.error("Exception while sending mail: ", e);
            throw new ClaimMailException("Exception while composing and sending mail with OTP");
        }

    }

    /**
     * @param mailDto
     * @return
     */
    private String generateCertificateMailContent(PendingMailDTO pendingMailDTO) {
        String processedTemplateString = null;

        Map<String, Object> mailMap = new HashMap<>();
        mailMap.put("name", pendingMailDTO.getName());
        mailMap.put("council", pendingMailDTO.getCouncil());
        mailMap.put("itemName", pendingMailDTO.getItemName());

        try {
            freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/templates/");
            Template template = freeMarkerConfiguration.getTemplate("pending-item-mail.ftl");
            processedTemplateString = FreeMarkerTemplateUtils.processTemplateIntoString(template, mailMap);

        } catch (TemplateException e) {
            logger.error("TemplateException while creating mail template for certificate ", e);
            throw new ClaimMailException("Error while creating mail template for certificate");
        } catch (IOException e) {
            logger.error("IOException while creating mail template for certificate ", e);
            throw new ClaimMailException("Error while creating mail template for certificate");
        }
        return processedTemplateString;
    }
}
