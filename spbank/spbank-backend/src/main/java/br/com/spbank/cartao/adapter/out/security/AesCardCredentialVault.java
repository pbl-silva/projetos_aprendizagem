package br.com.spbank.cartao.adapter.out.security;

import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.out.CardCredentialVault;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class AesCardCredentialVault implements CardCredentialVault {
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec fingerprintKey;

    public AesCardCredentialVault(@Value("${spbank.card.data-secret}") String secret) {
        if (secret == null || secret.length() < 32) throw new IllegalArgumentException("CARD_DATA_SECRET deve ter pelo menos 32 caracteres");
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            encryptionKey = new SecretKeySpec(digest.digest(("spbank:card:data:" + secret).getBytes(StandardCharsets.UTF_8)), "AES");
            fingerprintKey = new SecretKeySpec(digest.digest(("spbank:card:fingerprint:" + secret).getBytes(StandardCharsets.UTF_8)), "HmacSHA256");
        } catch (Exception ex) { throw new IllegalStateException("Proteção de cartão indisponível", ex); }
    }

    @Override public CardCredentials issue(CardBrand brand) {
        // Fictional study prefixes; these instruments cannot be used on payment networks.
        var number = new StringBuilder(switch (brand) {
            case BRISA -> "9901"; case MISTERSHARD -> "9902"; case ELU -> "9903";
        });
        while (number.length() < 15) number.append(random.nextInt(10));
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            int digit = number.charAt(i) - '0';
            if (i % 2 == 0) { digit *= 2; if (digit > 9) digit -= 9; }
            sum += digit;
        }
        number.append((10 - sum % 10) % 10);
        String pan = number.toString();
        String cvv = "%03d".formatted(random.nextInt(1000));
        try {
            var mac = Mac.getInstance("HmacSHA256"); mac.init(fingerprintKey);
            return new CardCredentials(encrypt(pan, "PAN"), encrypt(cvv, "CVV"),
                HexFormat.of().formatHex(mac.doFinal(pan.getBytes(StandardCharsets.UTF_8))), pan.substring(12));
        } catch (Exception ex) { throw new IllegalStateException("Não foi possível emitir credenciais", ex); }
    }

    @Override public CardDetails reveal(CardInstrument card) {
        return new CardDetails(card.holderName(), decrypt(card.encryptedPan(), "PAN"),
            decrypt(card.encryptedCvv(), "CVV"), card.expiresOn());
    }

    private String encrypt(String value, String purpose) throws Exception {
        byte[] nonce = new byte[12]; random.nextBytes(nonce);
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(("spbank:card:" + purpose).getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] envelope = Arrays.copyOf(nonce, nonce.length + encrypted.length);
        System.arraycopy(encrypted, 0, envelope, nonce.length, encrypted.length);
        return "sp1." + Base64.getEncoder().encodeToString(envelope);
    }

    private String decrypt(String envelope, String purpose) {
        try {
            if (!envelope.startsWith("sp1.")) throw new IllegalArgumentException();
            byte[] bytes = Base64.getDecoder().decode(envelope.substring(4));
            if (bytes.length < 29) throw new IllegalArgumentException();
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, Arrays.copyOf(bytes, 12)));
            cipher.updateAAD(("spbank:card:" + purpose).getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(Arrays.copyOfRange(bytes, 12, bytes.length)), StandardCharsets.UTF_8);
        } catch (Exception ex) { throw new IllegalStateException("Não foi possível revelar os dados do cartão"); }
    }
}
