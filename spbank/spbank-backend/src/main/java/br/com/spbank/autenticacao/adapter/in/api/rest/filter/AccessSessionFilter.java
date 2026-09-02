package br.com.spbank.autenticacao.adapter.in.api.rest.filter;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ACCESS_TOKEN;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ACCOUNT_ID;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;

import br.com.spbank.autenticacao.application.port.in.AuthenticationUseCase;
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
public final class AccessSessionFilter
        extends OncePerRequestFilter {

    private final AuthenticationUseCase authentication;
    private final HandlerExceptionResolver exceptionResolver;

    public AccessSessionFilter(
            AuthenticationUseCase authentication,
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver exceptionResolver
    ) {
        this.authentication = authentication;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        String path =
                request.getRequestURI();

        return "OPTIONS".equalsIgnoreCase(
                request.getMethod()
        )
                || !path.startsWith("/api/")
                || "/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path);
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
            var session =
                    authentication.resolveSession(
                            token
                    );

            request.setAttribute(
                    CUSTOMER_ID,
                    session.customerId()
            );

            request.setAttribute(
                    ACCOUNT_ID,
                    session.accountId()
            );

            request.setAttribute(
                    ACCESS_TOKEN,
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