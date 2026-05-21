package dev.sunbirdrc.claim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "dev.sunbirdrc.registry.middleware", "dev.sunbirdrc.claim"})
public class ClaimsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClaimsApplication.class, args);
    }
}
