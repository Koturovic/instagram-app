package com.instagram.user_service.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithCurrentUserSecurityContextFactory.class)
public @interface WithCurrentUser {

    long userId() default 1L;
    String username() default "testuser";
}
