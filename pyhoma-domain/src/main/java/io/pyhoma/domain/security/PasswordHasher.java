package io.pyhoma.domain.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 600_000;
    private static final int MIN_ITERATIONS = 100_000;
    private static final int MAX_ITERATIONS = 5_000_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DUMMY_HASH = hash("pyhoma-dummy-password-for-timing-1");

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON = Set.of(
            "password", "passw0rd", "senha", "senha123", "123456", "1234567890", "qwerty", "qwerty123", "abc123",
            "admin", "admin123", "administrator", "administrador", "letmein", "welcome", "bemvindo", "iloveyou",
            "pyhoma", "changeme", "mudar123", "trocar123", "teste", "test", "suporte", "support", "helpdesk");

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || password.length() > MAX_LENGTH || stored == null) {
            verifyDummy(password);
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            verifyDummy(password);
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
                verifyDummy(password);
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            verifyDummy(password);
            return false;
        }
    }

    public static void verifyDummy(String password) {
        String p = password == null ? "" : password.substring(0, Math.min(password.length(), MAX_LENGTH));
        String[] parts = DUMMY_HASH.split("\\$");
        derive(p, Base64.getDecoder().decode(parts[2]), ITERATIONS);
    }

    public static boolean needsRehash(String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        try {
            return parts.length != 4 || Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public static boolean isStrongEnough(String password) {
        return isAcceptable(password, null, null);
    }

    public static boolean isAcceptable(String password, String login, String fullName) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        if (password.isBlank() || !password.equals(password.strip())) {
            return false;
        }
        boolean letter = password.chars().anyMatch(Character::isLetter);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) {
            return false;
        }
        if (password.chars().distinct().count() < 5) {
            return false;
        }
        String folded = fold(password);
        String leet = leet(folded);
        for (String candidate : new String[] {folded, folded.replaceAll("[^a-z0-9]", ""), folded.replaceAll("[^a-z]", ""),
            leet, leet.replaceAll("[^a-z]", "")}) {
            if (COMMON.contains(candidate)) {
                return false;
            }
        }
        if (containsPart(folded, login)) {
            return false;
        }
        if (fullName != null) {
            for (String part : fullName.split("\\s+")) {
                if (part.length() >= 4 && containsPart(folded, part)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean containsPart(String foldedPassword, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String v = fold(value).replaceAll("[^a-z0-9]", "");
        if (v.length() < 3) {
            return false;
        }
        return foldedPassword.replaceAll("[^a-z0-9]", "").contains(v) || leet(foldedPassword).replaceAll("[^a-z0-9]", "").contains(v);
    }

    private static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String leet(String s) {
        return s.replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a').replace('5', 's').replace('@', 'a')
                .replace('$', 's');
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(chars, '\0');
        }
    }

    static String fingerprint(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(d).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
