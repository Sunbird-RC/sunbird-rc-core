package dev.sunbirdrc;


import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@SpringBootApplication
public class UserManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }

//    @Bean
//    public KeycloakSpringBootConfigResolver keycloakSpringBootConfigResolver(){
//        return new KeycloakSpringBootConfigResolver();
//    }
}