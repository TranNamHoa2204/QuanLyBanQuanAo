package common.dto;

import common.enums.VaiTro;

/** Request-only; mật khẩu ban đầu không bao giờ được trả lại trong response. */
public class TaiKhoanCreateRequest {
    private String maNV;
    private String username;
    private String matKhauBanDau;
    private VaiTro vaiTro;
    public TaiKhoanCreateRequest() { }
    public String getMaNV() { return maNV; }
    public void setMaNV(String maNV) { this.maNV = maNV; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMatKhauBanDau() { return matKhauBanDau; }
    public void setMatKhauBanDau(String matKhauBanDau) { this.matKhauBanDau = matKhauBanDau; }
    public VaiTro getVaiTro() { return vaiTro; }
    public void setVaiTro(VaiTro vaiTro) { this.vaiTro = vaiTro; }
}
