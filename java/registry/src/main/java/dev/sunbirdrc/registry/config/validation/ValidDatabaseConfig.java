package dev.sunbirdrc.registry.config.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({METHOD, FIELD, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = {DatabaseConfigValidator.class})
public @interface ValidDatabaseConfig {

    String message() default "{Invalid database properties}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
