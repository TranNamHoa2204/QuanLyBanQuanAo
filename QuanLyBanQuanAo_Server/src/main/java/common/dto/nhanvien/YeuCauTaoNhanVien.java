package common.dto.nhanvien;

import common.dto.taikhoan.YeuCauTaoTaiKhoan;

/** Có thể tạo nhân viên cùng tài khoản trong một request/transaction. */
public class YeuCauTaoNhanVien {
    private DuLieuNhanVien nhanVien;
    private YeuCauTaoTaiKhoan taiKhoan;

    public YeuCauTaoNhanVien() {
    }

    public DuLieuNhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(DuLieuNhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public YeuCauTaoTaiKhoan getTaiKhoan() {
        return taiKhoan;
    }

    public void setTaiKhoan(YeuCauTaoTaiKhoan taiKhoan) {
        this.taiKhoan = taiKhoan;
    }
}
