package common.dto;

/** Có thể tạo nhân viên cùng tài khoản trong một request/transaction. */
public class NhanVienCreateRequest {
    private NhanVienDTO nhanVien;
    private TaiKhoanCreateRequest taiKhoan;
    public NhanVienCreateRequest() { }
    public NhanVienDTO getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVienDTO nhanVien) { this.nhanVien = nhanVien; }
    public TaiKhoanCreateRequest getTaiKhoan() { return taiKhoan; }
    public void setTaiKhoan(TaiKhoanCreateRequest taiKhoan) { this.taiKhoan = taiKhoan; }
}
