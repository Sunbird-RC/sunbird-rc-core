package io.opensaber.registry.middleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;



/**
 * 
 * @author jyotsna
 *
 */
@Controller
@SpringBootApplication
@ComponentScan({"io.opensaber.registry.middleware"})
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
