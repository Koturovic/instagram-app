package com.instagram.user_service.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithCurrentUserSecurityContextFactory implements WithSecurityContextFactory<WithCurrentUser> {

    @Override
    public SecurityContext createSecurityContext(WithCurrentUser withUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        CurrentUser principal = new CurrentUser(withUser.userId(), withUser.username());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        context.setAuthentication(auth);
        return context;
    }
}
