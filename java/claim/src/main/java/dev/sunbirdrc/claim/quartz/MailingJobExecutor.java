package dev.sunbirdrc.claim.quartz;

import dev.sunbirdrc.claim.service.EmailService;
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

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info(">>>>>>>>>>>> Mail for foreign council job started :: " + new Date(System.currentTimeMillis()) + " <<<<<<<<<<<<<<<<");

        emailService.sendForeignPendingItemMail();

        log.info(">>>>>>>>>>>> Mail for foreign council has been completed  :: " + new Date(System.currentTimeMillis()) + " <<<<<<<<<<<<<<<<");
    }
}
