package io.opensaber.registry.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"io.opensaber.registry"})
public class OpenSaberApplication {

    private static Logger logger = LoggerFactory.getLogger(OpenSaberApplication.class);
    public static void main(String[] args) {
        logger.info("****************************************************");
        logger.info("         Welcome to Opensaber Application                     ");
        logger.info("****************************************************");

        SpringApplication.run(OpenSaberApplication.class, args);
    }




}
