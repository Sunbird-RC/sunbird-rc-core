package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.config.PropertyMapper;
import dev.sunbirdrc.claim.controller.EmailController;
import dev.sunbirdrc.claim.dto.PendingMailDTO;
import dev.sunbirdrc.claim.entity.Regulator;
import dev.sunbirdrc.claim.exception.ClaimMailException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AsyncMailSender {
    private static final Logger logger = LoggerFactory.getLogger(AsyncMailSender.class);
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PropertyMapper propertyMapper;

    @Autowired
    private Configuration freeMarkerConfiguration;

    /**
     * @param pendingMailDTOList
     * @param regulatorList
     */
    @Async
    public void sendPendingMailToRegulatorList(@NonNull List<PendingMailDTO> pendingMailDTOList,
                                               List<Regulator> regulatorList) {

        for (Regulator regulator : regulatorList) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();

                MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

                mimeMessageHelper.setSubject(propertyMapper.getForeignPendingItemSubject());
                mimeMessageHelper.setFrom(new InternetAddress(propertyMapper.getSimpleMailMessageFrom(),
                        "Auto generated mail for pending item"));
                mimeMessageHelper.setTo(regulator.getEmail());
                mimeMessageHelper.setText(generatePendingMailContent(pendingMailDTOList, regulator.getName()), true);

                mailSender.send(mimeMessageHelper.getMimeMessage());
            } catch (Exception e) {
                logger.error("Exception while sending mail: ", e);
                throw new ClaimMailException("Exception while composing and sending mail with OTP");
            }
        }
    }

    /**
     * @param pendingMailDTOList
     * @param regulatorName
     * @return
     */
    private String generatePendingMailContent(@NonNull List<PendingMailDTO> pendingMailDTOList,
                                              @NonNull String regulatorName) {
        String processedTemplateString = null;

        Map<String, Object> mailMap = new HashMap<>();
        mailMap.put("candidates", pendingMailDTOList);
        mailMap.put("regulatorName", regulatorName);

        try {
            freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/templates/");
            Template template = freeMarkerConfiguration.getTemplate("pending-item-mail.ftl");
            processedTemplateString = FreeMarkerTemplateUtils.processTemplateIntoString(template, mailMap);

        } catch (TemplateException e) {
            logger.error("TemplateException while creating auto mail template for foreing pending item ", e);
            throw new ClaimMailException("Error while creating auto mail template for foreing pending item");
        } catch (IOException e) {
            logger.error("IOException while creating auto mail template for foreing pending item ", e);
            throw new ClaimMailException("Error while creating auto mail template for foreing pending item");
        }
        return processedTemplateString;
    }

    /**
     * @param pendingMailDTO
     * @param regulatorName
     * @param regulatorEmail
     */
    @Async
    public void sendManualPendingMail(PendingMailDTO pendingMailDTO, @NonNull String regulatorName,
                                      @NonNull String regulatorEmail) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setSubject(propertyMapper.getForeignPendingItemSubject());
            mimeMessageHelper.setFrom(new InternetAddress(propertyMapper.getSimpleMailMessageFrom(),
                    "Pending action item"));
            mimeMessageHelper.setTo(regulatorEmail);
            mimeMessageHelper.setText(generateManualForeignPendingMailContent(pendingMailDTO, regulatorName), true);

            mailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (Exception e) {
            logger.error("Exception while sending mail: ", e);
            throw new ClaimMailException("Exception while composing and sending mail with OTP");
        }

    }

    /**
     * @param pendingMailDTO
     * @param foreignRegulatorName
     * @return
     */
    private String generateManualForeignPendingMailContent(PendingMailDTO pendingMailDTO, String foreignRegulatorName) {
        String processedTemplateString = null;

        Map<String, Object> mailMap = new HashMap<>();
        mailMap.put("candidate", pendingMailDTO);
        mailMap.put("regulatorName", foreignRegulatorName);

        try {
            freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/templates/");
            Template template = freeMarkerConfiguration.getTemplate("manual-pending-item-mail.ftl");
            processedTemplateString = FreeMarkerTemplateUtils.processTemplateIntoString(template, mailMap);

        } catch (TemplateException e) {
            logger.error("TemplateException while creating manual mail template for foreing pending item ", e);
            throw new ClaimMailException("Error while creating manual mail template for foreing pending item");
        } catch (IOException e) {
            logger.error("IOException while creating manual mail template for foreing pending item ", e);
            throw new ClaimMailException("Error while creating manual mail template for foreing pending item");
        }
        return processedTemplateString;
    }
}
