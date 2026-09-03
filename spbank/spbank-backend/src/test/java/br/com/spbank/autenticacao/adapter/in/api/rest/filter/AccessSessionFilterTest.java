package br.com.spbank.autenticacao.adapter.in.api.rest.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import br.com.spbank.autenticacao.application.port.in.AuthenticationUseCase;
import br.com.spbank.shared.application.exception.UnauthorizedException;

import jakarta.servlet.FilterChain;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class AccessSessionFilterTest {

    @Test
    void shouldLeaveAdministrativeRoutesToTheirOwnFilter()
            throws Exception {

        AuthenticationUseCase authentication =
                mock(AuthenticationUseCase.class);

        HandlerExceptionResolver resolver =
                mock(HandlerExceptionResolver.class);

        FilterChain chain =
                mock(FilterChain.class);

        var request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/v1/admin/accounts"
                );

        var response =
                new MockHttpServletResponse();

        new AccessSessionFilter(
                authentication,
                resolver
        ).doFilter(
                request,
                response,
                chain
        );

        verify(chain)
                .doFilter(
                        request,
                        response
                );

        verifyNoInteractions(
                authentication,
                resolver
        );
    }

    @Test
    void shouldDelegateAuthenticationFailureToTheGlobalExceptionResolver()
            throws Exception {

        AuthenticationUseCase authentication =
                mock(AuthenticationUseCase.class);

        when(
                authentication.resolveSession(
                        isNull()
                )
        )
                .thenThrow(
                        new UnauthorizedException()
                );

        AtomicReference<Exception> resolved =
                new AtomicReference<>();

        HandlerExceptionResolver resolver =
                (
                        request,
                        response,
                        handler,
                        exception
                ) -> {

                    resolved.set(
                            exception
                    );

                    return new ModelAndView();
                };

        FilterChain chain =
                mock(FilterChain.class);

        var request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/v1/accounts/me"
                );

        var response =
                new MockHttpServletResponse();

        new AccessSessionFilter(
                authentication,
                resolver
        ).doFilter(
                request,
                response,
                chain
        );

        assertThat(
                resolved.get()
        )
                .isInstanceOf(
                        UnauthorizedException.class
                );

        verifyNoInteractions(chain);
    }

    @Test
    void shouldRejectAnAdministrativeTokenOnAClientRoute()
            throws Exception {

        AuthenticationUseCase authentication =
                mock(AuthenticationUseCase.class);

        when(
                authentication.resolveSession(
                        "admin-token"
                )
        )
                .thenThrow(
                        new UnauthorizedException()
                );

        AtomicReference<Exception> resolved =
                new AtomicReference<>();

        HandlerExceptionResolver resolver =
                (
                        request,
                        response,
                        handler,
                        exception
                ) -> {

                    resolved.set(
                            exception
                    );

                    return new ModelAndView();
                };

        FilterChain chain =
                mock(FilterChain.class);

        var request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/v1/accounts/me"
                );

        request.addHeader(
                "Authorization",
                "Bearer admin-token"
        );

        var response =
                new MockHttpServletResponse();

        new AccessSessionFilter(
                authentication,
                resolver
        ).doFilter(
                request,
                response,
                chain
        );

        assertThat(
                resolved.get()
        )
                .isInstanceOf(
                        UnauthorizedException.class
                );

        verify(authentication)
                .resolveSession(
                        "admin-token"
                );

        verifyNoInteractions(chain);
    }
}