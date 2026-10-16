package common.dto;

/** Hồ sơ nhân viên và phần tài khoản đã lọc an toàn; tuyệt đối không có password/hash. */
public class NhanVienRecordDTO {
    private NhanVienDTO nhanVien;
    private TaiKhoanDTO taiKhoan;

    public NhanVienRecordDTO() { }
    public NhanVienDTO getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVienDTO nhanVien) { this.nhanVien = nhanVien; }
    public TaiKhoanDTO getTaiKhoan() { return taiKhoan; }
    public void setTaiKhoan(TaiKhoanDTO taiKhoan) { this.taiKhoan = taiKhoan; }
}
