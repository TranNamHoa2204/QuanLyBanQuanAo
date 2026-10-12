package common.dto;
import common.enums.VaiTro;

/** Thông tin nhân viên và quyền đăng nhập; tuyệt đối không chứa passwordHash. */
public class NhanVienAccountDTO extends NhanVienDTO {
    private String username;
    private VaiTro vaiTro;
    private boolean taiKhoanHoatDong = true;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public VaiTro getVaiTro() { return vaiTro; }
    public void setVaiTro(VaiTro vaiTro) { this.vaiTro = vaiTro; }
    public boolean getTaiKhoanHoatDong() { return taiKhoanHoatDong; }
    public void setTaiKhoanHoatDong(boolean value) { taiKhoanHoatDong = value; }
}
