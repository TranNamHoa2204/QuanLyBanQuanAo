package server.security;

/** TODO: chọn thư viện băm mật khẩu có salt; không dùng SHA-256 thuần hoặc mật khẩu rõ. */
public interface PasswordHasher {
    String hash(char[] password);

    boolean verify(char[] password, String encodedHash);
}
