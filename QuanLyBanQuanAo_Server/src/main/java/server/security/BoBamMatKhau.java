package server.security;

/** Hợp đồng băm và kiểm tra mật khẩu; triển khai PBKDF2 với salt riêng. */
public interface BoBamMatKhau {
    String hash(char[] password);

    boolean verify(char[] password, String encodedHash);
}
