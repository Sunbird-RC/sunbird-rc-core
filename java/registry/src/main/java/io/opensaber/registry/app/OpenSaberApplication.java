package io.opensaber.registry.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "io.opensaber.registry", "io.opensaber.pojos"})
public class OpenSaberApplication {
	public static void main(String[] args) {
		SpringApplication.run(OpenSaberApplication.class, args);
	}
}
