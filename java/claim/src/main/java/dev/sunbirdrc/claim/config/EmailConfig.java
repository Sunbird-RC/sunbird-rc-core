package dev.sunbirdrc.claim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;

@Configuration
public class EmailConfig 
{
	@Bean
	public SimpleMailMessage emailTemplate()
	{
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo("kumarpawans@gmail.com");
		message.setFrom("shishir.suman@tarento.com");
		message.setSubject("Identity Card link for UPSMF");
	    message.setText("FATAL - Application crash. Save your job !!");
	    return message;
	}
}