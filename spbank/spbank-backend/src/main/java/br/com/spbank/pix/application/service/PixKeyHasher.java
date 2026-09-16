package br.com.spbank.pix.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class PixKeyHasher {

    private PixKeyHasher() {
    }

    public static String hash(
        String normalizedKey
    ) {
        Objects.requireNonNull(
            normalizedKey,
            "normalizedKey"
        );

        try {
            byte[] digest = MessageDigest
                .getInstance("SHA-256")
                .digest(
                    normalizedKey.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            return HexFormat
                .of()
                .formatHex(digest);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                "SHA-256 indisponível",
                ex
            );
        }
    }
}