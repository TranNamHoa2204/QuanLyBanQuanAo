package server.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2-HMAC-SHA256; lưu thuật toán, số vòng, salt và hash. */
public final class Pbkdf2PasswordHasher implements PasswordHasher {
    private static final int ITERATIONS = 600000;
    private final SecureRandom random = new SecureRandom();
    @Override public String hash(char[] password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return "pbkdf2-sha256$" + ITERATIONS + "$" + encode(salt) + "$"
                + encode(derive(password, salt, ITERATIONS));
    }
    @Override public boolean verify(char[] password, String encodedHash) {
        if (password == null || encodedHash == null) { return false; }
        try {
            String[] parts = encodedHash.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) { return false; }
            int count = Integer.parseInt(parts[1]);
            if (count < 100000 || count > 1000000) { return false; }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length != 16 || expected.length != 32) { return false; }
            byte[] actual = derive(password, salt, count);
            boolean matches = MessageDigest.isEqual(expected, actual);
            Arrays.fill(actual, (byte) 0);
            return matches;
        } catch (IllegalArgumentException invalid) { return false; }
    }
    private static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
    private static byte[] derive(char[] password, byte[] salt, int count) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, count, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (java.security.GeneralSecurityException failure) {
            throw new IllegalStateException("JDK không hỗ trợ PBKDF2-HMAC-SHA256.", failure);
        } finally { spec.clearPassword(); }
    }
}
