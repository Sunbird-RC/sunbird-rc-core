package dev.sunbirdrc.registry.app;

import dev.sunbirdrc.registry.util.DefinitionsManager;
import dev.sunbirdrc.registry.util.DistributedDefinitionsManager;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

@SpringBootApplication(exclude={SecurityAutoConfiguration.class})
@ComponentScan(basePackages = {"dev.sunbirdrc.registry", "dev.sunbirdrc.pojos", "dev.sunbirdrc.keycloak", "dev.sunbirdrc.workflow", "dev.sunbirdrc.plugin"})
public class SunbirdRCApplication {
    private static ApplicationContext context;
    private static SpringApplication application = new SpringApplication(SunbirdRCApplication.class);
    @Value("${registry.manager.type}")
    private String definitionManagerType;

    @Value("${registry.redis.host:localhost}")
    private String redisHost;
    @Value("${registry.redis.port:6379}")
    private String redisPort;


    public static void main(String[] args) {
        context = application.run(args);
    }

    /**
     * This method return non-web application context
     *
     * @return context
     */
    public static ApplicationContext getAppContext() {
        application.setWebApplicationType(WebApplicationType.NONE);
        context = application.run();
        return context;
    }

    @Value("${cors.allowedOrigin}")
    public String corsAllowedOrigin;

    @Bean
    public FilterRegistrationBean corsFilter() {
        List<String> list = new ArrayList();
        list.add("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        //config.setAllowCredentials(true);
          config.addAllowedOrigin(corsAllowedOrigin);
          config.addAllowedOrigin("*");
          config.setAllowedOrigins(list);
          config.addAllowedHeader("*");
          config.addAllowedMethod("POST");
          config.addAllowedMethod("DELETE");
          config.addAllowedMethod("GET");
          config.addAllowedMethod("OPTIONS");
          config.addAllowedMethod("PUT");
          source.registerCorsConfiguration("/**", config);
          FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }

    @Bean
    @ConditionalOnProperty(value = "registry.manager.type", havingValue = "DistributedDefinitionsManager")
    public JedisPool jedisPool() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, redisHost, Integer.parseInt(redisPort));
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return jedisPool;
    }

    @Bean
    public IDefinitionsManager definitionsManager() {
        if(definitionManagerType.equals("DefinitionsManager")) {
            return new DefinitionsManager();
        }
        return new DistributedDefinitionsManager();
    }
}
