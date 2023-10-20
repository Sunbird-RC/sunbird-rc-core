package org.egov;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.egov.id.masterdata.MasterDataProvider;
import org.egov.id.utils.Constants;
import org.egov.tracer.config.TracerConfiguration;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.ObjectUtils;

/**
 * Description : This is initialization class for pt-idGeneration module
 * 
 * @author Pavan Kumar Kamma
 *
 */
@SpringBootApplication
@Slf4j
@Import({TracerConfiguration.class})
public class PtIdGenerationApplication {

	@Value("${egov.mdms.provider}")
	private String masterDataProviderClassName;
	public static void main(String[] args) {
		SpringApplication.run(PtIdGenerationApplication.class, args);
	}

	@Bean
	public MasterDataProvider masterDataProvider() {
		MasterDataProvider masterDataProvider = null;
		try{
			if(ObjectUtils.isEmpty(masterDataProviderClassName)){
				masterDataProviderClassName = Constants.DEFAULT_MASTER_DATA_PROVIDER;
			}
			Class<?> masterDataProviderClass = Class.forName(masterDataProviderClassName);

			masterDataProvider = (MasterDataProvider) masterDataProviderClass.newInstance();
			log.info("Invoked MasterDataProvider with Classname: {}", masterDataProviderClassName);
		} catch(ClassNotFoundException | InstantiationException | IllegalAccessException e){
			log.error("MDMS provider class {} cannot be instantiate with exception: {}", masterDataProviderClassName, ExceptionUtils.getStackTrace(e));
			throw new CustomException("Unable to load MDMS provider class", "MDMS Provider Init Exception");
		}
        return masterDataProvider;
	}
}
