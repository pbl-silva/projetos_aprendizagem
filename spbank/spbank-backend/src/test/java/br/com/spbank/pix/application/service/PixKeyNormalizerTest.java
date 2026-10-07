package br.com.spbank.pix.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.shared.application.exception.BusinessException;

import org.junit.jupiter.api.Test;

class PixKeyNormalizerTest {

    @Test
    void shouldNormalizeAllSupportedKeyTypes() {
        assertThat(
            PixKeyNormalizer.normalize(
                PixKeyType.CPF_CNPJ,
                "529.982.247-25"
            )
        ).isEqualTo(
            "52998224725"
        );

        assertThat(
            PixKeyNormalizer.normalize(
                PixKeyType.EMAIL,
                " Roberta@SPBank.Local "
            )
        ).isEqualTo(
            "roberta@spbank.local"
        );

        assertThat(
            PixKeyNormalizer.normalize(
                PixKeyType.PHONE,
                "(11) 99999-0002"
            )
        ).isEqualTo(
            "+5511999990002"
        );

        assertThat(
            PixKeyNormalizer.normalize(
                PixKeyType.RANDOM,
                "32222222-2222-4222-8222-222222222224"
            )
        ).isEqualTo(
            "32222222-2222-4222-8222-222222222224"
        );
    }

    @Test
    void shouldMaskKeysAndRejectInvalidValues() {
        assertThat(
            PixKeyNormalizer.mask(
                PixKeyType.EMAIL,
                "roberta@spbank.local"
            )
        ).isEqualTo(
            "r***@spbank.local"
        );

        assertThat(
            PixKeyNormalizer.mask(
                PixKeyType.CPF_CNPJ,
                "11144477735"
            )
        )
        .doesNotContain("1114447")
        .endsWith("7735");

        assertThatExceptionOfType(
            BusinessException.class
        )
        .isThrownBy(
            () ->
                PixKeyNormalizer.normalize(
                    PixKeyType.EMAIL,
                    "invalido"
                )
        )
        .satisfies(
            ex ->
                assertThat(
                    ex.getCode()
                ).isEqualTo(
                    "INVALID_PIX_KEY"
                )
        );
    }

    @Test
    void shouldValidateDocumentDigitsBrazilianMobileAndEmailDomain() {
        assertThat(
            PixKeyNormalizer.normalize(
                PixKeyType.CPF_CNPJ,
                "11.222.333/0001-81"
            )
        ).isEqualTo(
            "11222333000181"
        );

        assertThat(
            PixKeyNormalizer.normalize(
                PixKeyType.PHONE,
                "+55 (21) 98888-7777"
            )
        ).isEqualTo(
            "+5521988887777"
        );

        assertThat(
            PixKeyNormalizer.normalize(
                PixKeyType.EMAIL,
                "cliente@instituicao.bank"
            )
        ).isEqualTo(
            "cliente@instituicao.bank"
        );

        assertThatThrownBy(
            () ->
                PixKeyNormalizer.normalize(
                    PixKeyType.CPF_CNPJ,
                    "529.982.247-24"
                )
        ).isInstanceOf(
            BusinessException.class
        );

        assertThatThrownBy(
            () ->
                PixKeyNormalizer.normalize(
                    PixKeyType.PHONE,
                    "+55 (20) 98888-7777"
                )
        ).isInstanceOf(
            BusinessException.class
        );

        assertThatThrownBy(
            () ->
                PixKeyNormalizer.normalize(
                    PixKeyType.PHONE,
                    "+55 (21) 3888-7777"
                )
        ).isInstanceOf(
            BusinessException.class
        );

        assertThatThrownBy(
            () ->
                PixKeyNormalizer.normalize(
                    PixKeyType.EMAIL,
                    "cliente@instituicao.123"
                )
        ).isInstanceOf(
            BusinessException.class
        );
    }
}