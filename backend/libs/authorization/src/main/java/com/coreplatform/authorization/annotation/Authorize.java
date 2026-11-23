package com.coreplatform.authorization.annotation;

import com.coreplatform.authorization.model.FreshnessLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorize {

    String resource();

    String action();

    String resourceIdParam() default "";

    FreshnessLevel freshness() default FreshnessLevel.LOW;
}
