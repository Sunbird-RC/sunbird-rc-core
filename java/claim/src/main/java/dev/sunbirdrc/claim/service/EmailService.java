package dev.sunbirdrc.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.claim.config.PropertyMapper;
import dev.sunbirdrc.claim.controller.EmailController;
import dev.sunbirdrc.claim.dto.CertificateMailDto;
import dev.sunbirdrc.claim.dto.PendingMailDTO;
import dev.sunbirdrc.claim.entity.Claim;
import dev.sunbirdrc.claim.entity.Regulator;
import dev.sunbirdrc.claim.exception.ClaimMailException;
import dev.sunbirdrc.claim.model.ClaimStatus;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.lang.NonNull;
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
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private ClaimService claimService;

    @Autowired
    private RegulatorService regulatorService;

    @Autowired
    private AsyncMailSender asyncMailSender;
 
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

    public void autoSendPendingItemMail(String entityName) {
        if (!regulatorService.isRegulatorTableExist()) {
            logger.error(">>>>>>>>>>> Unable to find regulator table in database: No further process will be occurred");
            return;
        }

        List<Claim> allClaimList = claimService.findAll();

        if (allClaimList != null && !allClaimList.isEmpty()) {
            List<String> pendingItemCouncilList = getPendingItemCouncilList(allClaimList, entityName);

            for (String councilName : pendingItemCouncilList) {

                List<Claim> councilClaims = allClaimList.stream()
                        .filter(claim -> councilName.equalsIgnoreCase(getCouncilName(claim.getPropertyData())))
                        .collect(Collectors.toList());

                List<PendingMailDTO> pendingMailDTOList = collectEntityDetailsForPendingItem(councilClaims, entityName);

                List<Regulator> regulatorList = regulatorService.findByCouncil(councilName);

                asyncMailSender.sendPendingMailToRegulatorList(pendingMailDTOList, regulatorList, entityName);
            }
        }
    }

    /**
     * @param claimList
     * @return
     */
    private @NonNull List<String> getPendingItemCouncilList(@NonNull List<Claim> claimList, @NonNull String entityName) {
        List<String> councilList = claimList.stream()
                .filter(claim -> entityName.equalsIgnoreCase(claim.getEntity()))
                .filter(claim -> ClaimStatus.OPEN.name().equalsIgnoreCase(claim.getStatus()))
                .map(claim -> getCouncilName(claim.getPropertyData()))
                .distinct()
                .collect(Collectors.toList());

        if (councilList == null) {
            logger.error(">>>>>>>> Unale to find any pending foreign council list");
            return Collections.emptyList();
        } else {
            return councilList;
        }
    }

    private @NonNull List<PendingMailDTO> collectEntityDetailsForPendingItem(@NonNull List<Claim> claimList, String entityName) {
        List<PendingMailDTO> pendingMailDTOList = new ArrayList<>();

        try {
            for (Claim claim : claimList) {
                String propertyData = claim.getPropertyData();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(propertyData);
                String entityId = claim.getEntityId().replace(propertyMapper.getRegistryShardId() + "-", "");

                PendingMailDTO pendingMailDTO = new PendingMailDTO();
                pendingMailDTO.setCredType(jsonNode.get("credType") != null ? jsonNode.get("credType").asText() : "");
                pendingMailDTO.setEmailAddress(jsonNode.get("email") != null ? jsonNode.get("email").asText() : "");
                pendingMailDTO.setName(jsonNode.get("name") != null ? jsonNode.get("name").asText() : "");


                if (propertyMapper.getStudentForeignEntityName().equalsIgnoreCase(entityName)) {
                    pendingMailDTO.setRefNo(jsonNode.get("refNo") != null ? jsonNode.get("refNo").asText() : "");
                    pendingMailDTO.setRegistrationNumber(jsonNode.get("registrationNumber") != null
                            ? jsonNode.get("registrationNumber").asText() : "");

                    pendingMailDTO.setVerifyLink(propertyMapper.getClaimUrl() + "/api/v1/foreignStudent/" + entityId);
                }

                if (propertyMapper.getStudentFromOutsideEntityName().equalsIgnoreCase(entityName)) {
                    pendingMailDTO.setNurseRegNo(jsonNode.get("nurseRegNo") != null
                            ? jsonNode.get("nurseRegNo").asText() : "");
                    pendingMailDTO.setRegistrationType(jsonNode.get("registrationType") != null
                            ? jsonNode.get("registrationType").asText() : "");

                    pendingMailDTO.setVerifyLink(propertyMapper.getClaimUrl() + "/api/v1/outsideStudent/" + entityId);
                }

                if (propertyMapper.getStudentFromUpEntityName().equalsIgnoreCase(entityName)) {
                    pendingMailDTO.setCourseName(jsonNode.get("courseName") != null ? jsonNode.get("courseName").asText() : "");
                    pendingMailDTO.setExamBody(jsonNode.get("examBody") != null ? jsonNode.get("examBody").asText() : "");
                }

                if (propertyMapper.getStudentGoodStandingEntityName().equalsIgnoreCase(entityName)) {
                    pendingMailDTO.setCourseName(jsonNode.get("workPlace") != null ? jsonNode.get("workPlace").asText() : "");
                }

                pendingMailDTOList.add(pendingMailDTO);
            }
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> Unable to read council name from claim property data", e);
        }
        return pendingMailDTOList;
    }

    private @NonNull String getCouncilName(String propertyData) {
        String council = "";
        if (StringUtils.isEmpty(propertyData)) {
            logger.error(">>>>>>> Error while fetching council name from property data in Claim");
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(propertyData);
            council = jsonNode.get("council").asText();
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> Unable to read council name from claim property data", e);
        }

        return council;
    }

    /**
     * @param claimId
     */
    public void collectAndSendForeignCoucilMailManually(String claimId) {
        if (!StringUtils.isEmpty(claimId)) {
            Optional<Claim> claimOptional = claimService.findById(claimId);

            if (claimOptional.isPresent()) {
                Claim claim = claimOptional.get();

                String foreignCouncilName = getCouncilName(claim.getPropertyData());

                Optional<PendingMailDTO> pendingMailDtoOptional = collectEntityDetailsForManualMail(claim);

                if (pendingMailDtoOptional.isPresent()) {
                    List<Regulator> regulatorList = regulatorService.findByCouncil(foreignCouncilName);

                    for (Regulator regulator : regulatorList) {
                        logger.info(">>>>>>>>>>>>>>> before sending pending mail");
                        asyncMailSender.sendManualPendingMail(pendingMailDtoOptional.get(), regulator.getName(), regulator.getEmail());
                        logger.info(">>>>>>>>>>>>>>>>>>> mail has been processed");
                    }
                } else {
                    logger.error(">>>>> Unable to collect entitiy details - while sending pending item mail to foreign council");
                }
            }
        }
    }

    /**
     * @param claim
     * @return
     */
    private Optional<PendingMailDTO> collectEntityDetailsForManualMail(@NonNull Claim claim) {
        try {
            String propertyData = claim.getPropertyData();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(propertyData);
            String entityId = claim.getEntityId().replace(propertyMapper.getRegistryShardId() + "-", "");

            PendingMailDTO pendingMailDTO = PendingMailDTO.builder()
                    .credType(jsonNode.get("credType") != null ? jsonNode.get("credType").asText() : "")
                    .emailAddress(jsonNode.get("email") != null ? jsonNode.get("email").asText() : "")
                    .refNo(jsonNode.get("refNo") != null ? jsonNode.get("refNo").asText() : "")
                    .name(jsonNode.get("name") != null ? jsonNode.get("name").asText() : "")
                    .registrationNumber(jsonNode.get("registrationNumber") != null ? jsonNode.get("registrationNumber").asText() : "")
                    .verifyLink(propertyMapper.getClaimUrl() + "/api/v1/outside/foreignStudent/" + entityId)
                    .build();

            return Optional.ofNullable(pendingMailDTO);

        } catch (Exception e) {
            logger.error(">>>>>>>>>>> Unable to read council name from claim property data", e);
        }

        return Optional.empty();
    }
}
