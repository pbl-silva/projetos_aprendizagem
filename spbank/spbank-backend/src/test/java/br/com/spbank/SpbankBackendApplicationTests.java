package br.com.spbank;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpbankBackendApplicationTests {

    @Test
    void applicationClassIsAvailable() {
        assertThat(SpbankBackendApplication.class).isNotNull();
    }
}