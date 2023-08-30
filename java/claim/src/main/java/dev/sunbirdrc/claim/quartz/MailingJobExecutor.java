package dev.sunbirdrc.claim.quartz;

import dev.sunbirdrc.claim.config.PropertyMapper;
import dev.sunbirdrc.claim.service.EmailService;
import dev.sunbirdrc.claim.service.StudentForeignVerificationService;
import dev.sunbirdrc.claim.service.StudentOutsideUpService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class MailingJobExecutor extends QuartzJobBean {

    @Autowired
    private EmailService emailService;

    @Autowired
    private StudentForeignVerificationService foreignVerificationService;

    @Autowired
    private StudentOutsideUpService studentOutsideUpService;

    @Autowired
    private PropertyMapper propertyMapper;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info(">>>>>>>>>>>> Mail for foreign council job started :: " + new Date(System.currentTimeMillis()) + " <<<<<<<<<<<<<<<<");

        if (foreignVerificationService.isStudentForeignVerificationTableExist()) {
            log.info(">>>>>>>>>>>>>>> Sending auto mail to foreign student pending item");
            emailService.autoSendPendingItemMail(propertyMapper.getStudentForeignEntityName());
        } else {
            log.error(">>>>>>>>>> Unable to find student foreign verification table - Further process will not be executed");
        }

        if (studentOutsideUpService.isStudentFromOutsideVerificationTableExist()) {
            log.info(">>>>>>>>>>>>>>> Sending auto mail to student from outside pending item");
            emailService.autoSendPendingItemMail(propertyMapper.getStudentFromOutsideEntityName());
        } else {
            log.error(">>>>>>>>>>> Unable to find student from outside verification table - Further process will not be executed");
        }

        log.info(">>>>>>>>>>>>>>> Sending auto mail to student from up pending item");
        emailService.autoSendPendingItemMail(propertyMapper.getStudentFromUpEntityName());

        log.info(">>>>>>>>>>>>>>> Sending auto mail to student good standing pending item");
        emailService.autoSendPendingItemMail(propertyMapper.getStudentGoodStandingEntityName());


        log.info(">>>>>>>>>>>> Mail for foreign council has been completed  :: " + new Date(System.currentTimeMillis()) + " <<<<<<<<<<<<<<<<");
    }
}
