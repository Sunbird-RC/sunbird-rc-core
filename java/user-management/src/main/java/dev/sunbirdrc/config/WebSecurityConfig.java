package dev.sunbirdrc.config;

import dev.sunbirdrc.utils.UserConstant;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);

//    @Override
//    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
//        return new NullAuthenticatedSessionStrategy();
//    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
//        http.csrf().disable()
//                .authorizeRequests()
//                .antMatchers("**/**/login").permitAll()
////                .antMatchers("/**/keycloak/**").hasAnyRole(UserConstant.ADMIN_ROLE)
//                .anyRequest()
//                .authenticated();

//        http.csrf().disable()
//                .authorizeRequests()
//                .antMatchers("/**/**/login").permitAll()
//                .and()
//                .authorizeRequests()
//                .antMatchers("/**/**/keycloak").hasRole(UserConstant.ADMIN_ROLE)
//                .anyRequest()
//                .authenticated()
//                .and()
//                .oauth2ResourceServer()
//                .jwt();

        http.csrf().disable()
                .authorizeRequests()
                .antMatchers("/**/keycloak/**", "/**/keycloak/**/**")
                .authenticated()
                .and()
                .oauth2ResourceServer()
                .jwt();


    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
