package common.dto;

import common.enums.VaiTro;

/** Đổi vai trò với điều kiện vai trò cũ để phát hiện dữ liệu lỗi thời. */
public class TaiKhoanRoleRequest {
    private String username;
    private VaiTro expectedVaiTro;
    private VaiTro vaiTro;
    public TaiKhoanRoleRequest() { }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public VaiTro getExpectedVaiTro() { return expectedVaiTro; }
    public void setExpectedVaiTro(VaiTro expectedVaiTro) { this.expectedVaiTro = expectedVaiTro; }
    public VaiTro getVaiTro() { return vaiTro; }
    public void setVaiTro(VaiTro vaiTro) { this.vaiTro = vaiTro; }
}
