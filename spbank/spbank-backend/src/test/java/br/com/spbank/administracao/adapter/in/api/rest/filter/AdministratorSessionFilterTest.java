package br.com.spbank.administracao.adapter.in.api.rest.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import br.com.spbank.administracao.application.port.in.AdministrationUseCase;
import br.com.spbank.shared.application.exception.UnauthorizedException;

import jakarta.servlet.FilterChain;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class AdministratorSessionFilterTest {

    @Test
    void shouldDelegateMissingAdministrativeSessionToTheExceptionResolver()
            throws Exception {

        AdministrationUseCase administration =
                mock(AdministrationUseCase.class);

        when(
                administration.resolveSession(
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
                        "/api/v1/admin/accounts"
                );

        var response =
                new MockHttpServletResponse();

        new AdministratorSessionFilter(
                administration,
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
    void shouldRejectAClientTokenOnAnAdministrativeRoute()
            throws Exception {

        AdministrationUseCase administration =
                mock(AdministrationUseCase.class);

        when(
                administration.resolveSession(
                        "client-token"
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
                        "/api/v1/admin/accounts"
                );

        request.addHeader(
                "Authorization",
                "Bearer client-token"
        );

        var response =
                new MockHttpServletResponse();

        new AdministratorSessionFilter(
                administration,
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

        verify(administration)
                .resolveSession(
                        "client-token"
                );

        verifyNoInteractions(chain);
    }
}