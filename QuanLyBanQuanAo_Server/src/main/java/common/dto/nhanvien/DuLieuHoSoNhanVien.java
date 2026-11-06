package common.dto.nhanvien;

import common.dto.taikhoan.DuLieuTaiKhoan;

/** Hồ sơ nhân viên và phần tài khoản đã lọc an toàn; tuyệt đối không có password/hash. */
public class DuLieuHoSoNhanVien {
    private DuLieuNhanVien nhanVien;
    private DuLieuTaiKhoan taiKhoan;

    public DuLieuHoSoNhanVien() {
    }

    public DuLieuNhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(DuLieuNhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public DuLieuTaiKhoan getTaiKhoan() {
        return taiKhoan;
    }

    public void setTaiKhoan(DuLieuTaiKhoan taiKhoan) {
        this.taiKhoan = taiKhoan;
    }
}
