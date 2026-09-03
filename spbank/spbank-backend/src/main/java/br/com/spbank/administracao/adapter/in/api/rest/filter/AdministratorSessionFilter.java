package br.com.spbank.administracao.adapter.in.api.rest.filter;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMINISTRATOR_ID;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMINISTRATOR_NAME;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMIN_ACCESS_TOKEN;

import br.com.spbank.administracao.application.port.in.AdministrationUseCase;
import br.com.spbank.shared.application.exception.UnauthorizedException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class AdministratorSessionFilter
        extends OncePerRequestFilter {

    private static final String ADMIN_PREFIX =
            "/api/v1/admin/";

    private final AdministrationUseCase administration;
    private final HandlerExceptionResolver exceptionResolver;

    public AdministratorSessionFilter(
            AdministrationUseCase administration,
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver exceptionResolver
    ) {
        this.administration = administration;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path =
                request.getRequestURI();

        return "OPTIONS"
                .equalsIgnoreCase(
                        request.getMethod()
                )
                || !path.startsWith(ADMIN_PREFIX)
                || "/api/v1/admin/auth/login"
                        .equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader(
                        "Authorization"
                );

        String token =
                authorization != null
                        && authorization.startsWith("Bearer ")
                        ? authorization
                                .substring(7)
                                .trim()
                        : null;

        try {

            var administrator =
                    administration.resolveSession(
                            token
                    );

            request.setAttribute(
                    ADMINISTRATOR_ID,
                    administrator.id()
            );

            request.setAttribute(
                    ADMINISTRATOR_NAME,
                    administrator.displayName()
            );

            request.setAttribute(
                    ADMIN_ACCESS_TOKEN,
                    token
            );

            chain.doFilter(
                    request,
                    response
            );

        } catch (UnauthorizedException ex) {

            exceptionResolver.resolveException(
                    request,
                    response,
                    null,
                    ex
            );
        }
    }
}