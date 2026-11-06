package common.dto.taikhoan;

import common.enums.VaiTro;

/** Đổi vai trò với điều kiện vai trò cũ để phát hiện dữ liệu lỗi thời. */
public class YeuCauVaiTroTaiKhoan {
    private String username;
    private VaiTro expectedVaiTro;
    private VaiTro vaiTro;

    public YeuCauVaiTroTaiKhoan() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public VaiTro getExpectedVaiTro() {
        return expectedVaiTro;
    }

    public void setExpectedVaiTro(VaiTro expectedVaiTro) {
        this.expectedVaiTro = expectedVaiTro;
    }

    public VaiTro getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTro vaiTro) {
        this.vaiTro = vaiTro;
    }
}
