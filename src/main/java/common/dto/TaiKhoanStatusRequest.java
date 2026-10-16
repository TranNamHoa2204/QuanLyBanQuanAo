package common.dto;

/** Khóa/mở khóa tài khoản có điều kiện trạng thái cũ. */
public class TaiKhoanStatusRequest {
    private String username;
    private Boolean expectedTrangThai;
    public TaiKhoanStatusRequest() { }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Boolean getExpectedTrangThai() { return expectedTrangThai; }
    public void setExpectedTrangThai(Boolean expectedTrangThai) { this.expectedTrangThai = expectedTrangThai; }
}
