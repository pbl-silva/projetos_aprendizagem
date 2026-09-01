package br.com.spbank.autenticacao.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM =
            new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(
            String password
    ) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "A senha não pode estar vazia"
            );
        }

        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );

            byte[] calculated =
                    SecretKeyFactory
                            .getInstance(
                                    "PBKDF2WithHmacSHA256"
                            )
                            .generateSecret(spec)
                            .getEncoded();

            spec.clearPassword();

            return "pbkdf2_sha256$"
                    + ITERATIONS
                    + "$"
                    + Base64.getEncoder()
                            .encodeToString(salt)
                    + "$"
                    + Base64.getEncoder()
                            .encodeToString(calculated);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "PBKDF2 indisponível",
                    ex
            );
        }
    }

    public static boolean matches(
            String password,
            String encoded
    ) {
        if (password == null || encoded == null) {
            return false;
        }

        try {
            String[] parts =
                    encoded.split("\\$", -1);

            if (parts.length != 4
                    || !"pbkdf2_sha256"
                            .equals(parts[0])) {
                return false;
            }

            int iterations =
                    Integer.parseInt(parts[1]);

            byte[] salt =
                    Base64.getDecoder()
                            .decode(parts[2]);

            byte[] expected =
                    Base64.getDecoder()
                            .decode(parts[3]);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    iterations,
                    expected.length * 8
            );

            byte[] calculated =
                    SecretKeyFactory
                            .getInstance(
                                    "PBKDF2WithHmacSHA256"
                            )
                            .generateSecret(spec)
                            .getEncoded();

            spec.clearPassword();

            return MessageDigest.isEqual(
                    expected,
                    calculated
            );

        } catch (Exception ignored) {
            return false;
        }
    }

    public static String tokenHash(
            String token
    ) {
        try {
            byte[] digest =
                    MessageDigest
                            .getInstance("SHA-256")
                            .digest(
                                    token.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            );

            return java.util.HexFormat
                    .of()
                    .formatHex(digest);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "SHA-256 indisponível",
                    ex
            );
        }
    }
}