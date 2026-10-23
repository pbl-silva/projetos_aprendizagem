package br.com.spbank.cartao.adapter.out.security;

import static org.assertj.core.api.Assertions.*;
import br.com.spbank.cartao.application.model.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AesCardCredentialVaultTest {
    private final AesCardCredentialVault vault=new AesCardCredentialVault("a-long-study-test-secret-with-more-than-32-characters");

    @Test void issuedCredentialsAreEncryptedUniqueAndRoundTripCorrectly() {
        var fingerprints=new HashSet<String>();
        for(var brand:CardBrand.values()) {
            var credentials=vault.issue(brand);
            var card=card(credentials);
            var details=vault.reveal(card);
            assertThat(details.pan()).matches("990[123][0-9]{12}");
        assertThat(details.pan()).endsWith(credentials.lastFour());
            assertThat(details.cvv()).matches("[0-9]{3}");
            assertThat(credentials.encryptedPan()).startsWith("sp1.");
        assertThat(credentials.encryptedPan()).doesNotContain(details.pan());
            assertThat(credentials.panFingerprint()).matches("[0-9a-f]{64}");
            assertThat(credentials.toString()).doesNotContain(details.pan(),details.cvv());
            assertThat(fingerprints.add(credentials.panFingerprint())).isTrue();
        }
    }
    @Test void tamperingOrWrongKeyFailsClosed() {
        var credentials=vault.issue(CardBrand.BRISA);
        var encoded=Base64.getDecoder().decode(credentials.encryptedPan().substring(4));
        encoded[encoded.length-1]^=1;
        var corrupt=new CardCredentials("sp1."+Base64.getEncoder().encodeToString(encoded),credentials.encryptedCvv(),credentials.panFingerprint(),credentials.lastFour());
        assertThatThrownBy(() -> vault.reveal(card(corrupt))).isInstanceOf(IllegalStateException.class);
        var wrong=new AesCardCredentialVault("a-different-long-secret-with-more-than-32-characters");
        assertThatThrownBy(() -> wrong.reveal(card(credentials))).isInstanceOf(IllegalStateException.class);
    }
    @Test void encryptedPanAndCvvCannotBeSwapped() {
        var credentials=vault.issue(CardBrand.BRISA);
        var swapped=new CardCredentials(credentials.encryptedCvv(),credentials.encryptedPan(),credentials.panFingerprint(),credentials.lastFour());
        assertThatThrownBy(() -> vault.reveal(card(swapped))).isInstanceOf(IllegalStateException.class);
    }
    @Test void requiresExplicitAdequateSecret() {
        assertThatThrownBy(() -> new AesCardCredentialVault("short")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AesCardCredentialVault(null)).isInstanceOf(IllegalArgumentException.class);
    }
    private CardInstrument card(CardCredentials credentials) {
        return new CardInstrument(UUID.randomUUID(),UUID.randomUUID(),CardBrand.BRISA,null,"Cliente",credentials.lastFour(),
            credentials.panFingerprint(),credentials.encryptedPan(),credentials.encryptedCvv(),null,CardState.PENDING_ACTIVATION,
            0,false,false,false,null,null,LocalDate.of(2029,10,31),Instant.now());
    }
}
