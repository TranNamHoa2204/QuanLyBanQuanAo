package common.dto.taikhoan;

/** Khóa/mở khóa tài khoản có điều kiện trạng thái cũ. */
public class YeuCauTrangThaiTaiKhoan {
    private String username;
    private Boolean expectedTrangThai;

    public YeuCauTrangThaiTaiKhoan() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getExpectedTrangThai() {
        return expectedTrangThai;
    }

    public void setExpectedTrangThai(Boolean expectedTrangThai) {
        this.expectedTrangThai = expectedTrangThai;
    }
}
