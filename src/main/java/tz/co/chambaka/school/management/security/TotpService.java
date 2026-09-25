package tz.co.chambaka.school.management.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;

@Component
public class TotpService {

    public static final String ISSUER = "ShuleHub 2FA";
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20;
    private static final int PERIOD_SECONDS = 30;
    private static final int WINDOW = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String newSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public String otpauthUri(String account, String secret) {
        String email = account == null || account.isBlank() ? "user" : account.trim();
        String label = url(ISSUER) + ":" + url(email);
        return "otpauth://totp/" + label
                + "?secret=" + url(normalizeSecret(secret))
                + "&issuer=" + url(ISSUER)
                + "&algorithm=SHA1&digits=6&period=" + PERIOD_SECONDS;
    }

    public boolean verify(String secret, String code) {
        String normalized = normalizeCode(code);
        byte[] key = decodeBase32(secret);
        if (key.length == 0 || normalized == null) {
            return false;
        }
        long counter = Instant.now().getEpochSecond() / PERIOD_SECONDS;
        boolean matched = false;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            String expected = generate(key, counter + i);
            if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                    normalized.getBytes(StandardCharsets.US_ASCII))) {
                matched = true;
            }
        }
        return matched;
    }

    static String generate(byte[] key, long counter) {
        byte[] data = new byte[8];
        long value = counter;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xff);
            value >>= 8;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("TOTP is not available", ex);
        }
    }

    static String normalizeSecret(String secret) {
        if (secret == null) {
            return "";
        }
        return secret.replaceAll("[^A-Za-z2-7]", "").toUpperCase();
    }

    private static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String digits = code.replaceAll("\\D", "");
        return digits.length() == 6 ? digits : null;
    }

    static String encodeBase32(byte[] data) {
        StringBuilder out = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(ALPHABET.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
        }
        return out.toString();
    }

    static byte[] decodeBase32(String secret) {
        String normalized = normalizeSecret(secret);
        if (normalized.isEmpty()) {
            return new byte[0];
        }
        int buffer = 0;
        int bits = 0;
        byte[] out = new byte[normalized.length() * 5 / 8];
        int index = 0;
        for (int i = 0; i < normalized.length(); i++) {
            int value = ALPHABET.indexOf(normalized.charAt(i));
            if (value < 0) {
                return new byte[0];
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                out[index++] = (byte) ((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out;
    }

    private static String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
