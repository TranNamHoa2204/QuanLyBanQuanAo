package common.dto.taikhoan;

import common.enums.VaiTro;

/** YeuCau-only; mật khẩu ban đầu không bao giờ được trả lại trong response. */
public class YeuCauTaoTaiKhoan {
    private String maNV;
    private String username;
    private String matKhauBanDau;
    private VaiTro vaiTro;

    public YeuCauTaoTaiKhoan() {
    }

    public String getMaNV() {
        return maNV;
    }

    public void setMaNV(String maNV) {
        this.maNV = maNV;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMatKhauBanDau() {
        return matKhauBanDau;
    }

    public void setMatKhauBanDau(String matKhauBanDau) {
        this.matKhauBanDau = matKhauBanDau;
    }

    public VaiTro getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTro vaiTro) {
        this.vaiTro = vaiTro;
    }
}
