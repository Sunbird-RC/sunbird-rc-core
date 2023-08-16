package dev.sunbirdrc.service;

import dev.sunbirdrc.config.PropertiesValueMapper;
import dev.sunbirdrc.dto.CustomUserDTO;
import dev.sunbirdrc.entity.UserDetails;
import dev.sunbirdrc.exception.OtpException;
import dev.sunbirdrc.utils.OtpUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class MailService {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private Configuration freeMarkerConfiguration;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PropertiesValueMapper propMapping;

    @Autowired
    private OtpUtil otpUtil;

    /**
     * @param userDetails
     */
    @Async
    public void sendOtpMail(UserDetails userDetails) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setSubject(propMapping.getOtpMailVerificationSubject());
            mimeMessageHelper.setFrom(new InternetAddress(propMapping.getOtpMailVerificationFromAddress(),
                    propMapping.getOtpMailVerificationPersonalName()));
            mimeMessageHelper.setTo(userDetails.getEmail());
            mimeMessageHelper.setText(generateMailContent(userDetails), true);

            mailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (Exception e) {
            logger.error("Exception while sending mail: ", e);
            throw new OtpException("Exception while composing and sending mail with OTP");
        }
    }


    /**
     * @param userProfile
     * @return
     * @throws Exception
     */
    private String generateMailContent(UserDetails userProfile) throws Exception {
        String processedTemplateString = null;


        Map<String, Object> mailMap = new HashMap<>();
        mailMap.put("userFirstName", userProfile.getFirstName());
        mailMap.put("userLastName", userProfile.getLastName());
        mailMap.put("signature", "UPSMF");
        mailMap.put("location", "");

        TimeUnit timeUnit = otpUtil.getOtpTimeUnit();
        Optional<Integer> optionalOTP = otpUtil.generateAndPersistOTP(userProfile.getUserId(), timeUnit);

        if (optionalOTP.isPresent()) {
            mailMap.put("otp", String.valueOf(optionalOTP.get()));
            mailMap.put("otpDuration", propMapping.getOtpTtlDuration());
            mailMap.put("timeUnit", timeUnit.name().toLowerCase());
        }

        freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/templates/");
        Template template = freeMarkerConfiguration.getTemplate("otp-verification-mail.ftl");

        try {
            processedTemplateString = FreeMarkerTemplateUtils.processTemplateIntoString(template, mailMap);
        } catch (TemplateException e) {
            logger.error("Error while creating mail template for request info");
            throw new Exception("Error while creating mail template for request info");
        }

        return processedTemplateString;
    }

    @Async
    public void sendUserCreationNotification(CustomUserDTO customUserDTO) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setSubject(propMapping.getCustomUserCreationSubject());
            mimeMessageHelper.setFrom(new InternetAddress(propMapping.getCustomUserCreationFromAddress(),
                    propMapping.getCustomUserCreationPersonalName()));
            mimeMessageHelper.setTo(customUserDTO.getEmail());
            mimeMessageHelper.setText(generateNotificationMailContent(customUserDTO), true);

            mailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (Exception e) {
            logger.error("Exception while sending user creation mail notification: ", e);
//            throw new Exception("Exception while composing and sending user creation mail notification");
        }
    }

    private String generateNotificationMailContent(CustomUserDTO customUserDTO) throws Exception {
        String processedTemplateString = null;


        Map<String, Object> mailMap = new HashMap<>();
        mailMap.put("userFirstName", customUserDTO.getFirstName());
        mailMap.put("userLastName", customUserDTO.getLastName());
        mailMap.put("loginLInk", propMapping.getCustomUserLoginUrl());
        mailMap.put("signature", "UPSMF");

        freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/templates/");
        Template template = freeMarkerConfiguration.getTemplate("user-creation-notification-mail.ftl");

        try {
            processedTemplateString = FreeMarkerTemplateUtils.processTemplateIntoString(template, mailMap);
        } catch (TemplateException e) {
            logger.error("Error while creating notification mail template for request info");
            throw new Exception("Error while creating notification mail template for request info");
        }

        return processedTemplateString;
    }
}
