package br.com.spbank.shared.adapter.in.api.rest.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class CorsConfigurationTest {

    @Test
    void shouldAllowEveryHttpMethodUsedByTheBrowserApplication() {

        var configuration =
                new br.com.spbank.shared.adapter.in.api.rest.config.CorsConfiguration(
                        new String[]{
                                "http://localhost:5500"
                        }
                );

        var registry =
                new InspectableCorsRegistry();

        configuration.addCorsMappings(
                registry
        );

        assertThat(
                registry
                        .configurations()
                        .get("/api/**")
                        .getAllowedMethods()
        )
                .containsExactly(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE"
                );
    }

    private static final class InspectableCorsRegistry
            extends CorsRegistry {

        private Map<String, CorsConfiguration>
                configurations() {

            return getCorsConfigurations();
        }
    }
}