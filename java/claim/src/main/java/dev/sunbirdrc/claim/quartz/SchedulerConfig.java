package dev.sunbirdrc.claim.quartz;

import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class SchedulerConfig {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QuartzProperties quartzProperties;

    @Autowired
    private JobCreator jobCreator;

    /**
     * create scheduler factory
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(@Qualifier("searchJobTrigger") Trigger searchJobTrigger) {

        SchedulerJobFactory jobFactory = new SchedulerJobFactory();
        jobFactory.setApplicationContext(applicationContext);

        Properties properties = new Properties();
        properties.putAll(quartzProperties.getProperties());

        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setOverwriteExistingJobs(true);
        factory.setDataSource(dataSource);
        factory.setQuartzProperties(properties);
        factory.setJobFactory(jobFactory);
        factory.setTriggers(searchJobTrigger);
        return factory;
    }

    @Bean(name = "searchJobTrigger")
    public CronTriggerFactoryBean sampleJobTrigger(@Qualifier("quartzJobDetail") JobDetail jobDetail,
                                                   @Value("${samplejob.frequency}") String frequency) {
        return createCronTrigger(jobDetail, frequency);
    }

    private static CronTriggerFactoryBean createCronTrigger(JobDetail jobDetail, String cronExpression) {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(jobDetail);
        factoryBean.setCronExpression(cronExpression);
        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
        return factoryBean;
    }

    @Bean
    public JobDetail quartzJobDetail() {
        return jobCreator.createJob(MailingJobExecutor.class, true, applicationContext, "Send mail", "Mail Event Group");
    }
}
