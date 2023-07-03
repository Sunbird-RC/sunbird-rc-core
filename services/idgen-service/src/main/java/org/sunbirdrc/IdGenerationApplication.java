package org.sunbirdrc;

import org.egov.tracer.config.TracerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Description : This is initialization class for pt-idGeneration module
 * 
 * @author Pavan Kumar Kamma
 *
 */
@SpringBootApplication
@Import({TracerConfiguration.class})
public class IdGenerationApplication {
	public static void main(String[] args) {
		SpringApplication.run(IdGenerationApplication.class, args);
	}
}
