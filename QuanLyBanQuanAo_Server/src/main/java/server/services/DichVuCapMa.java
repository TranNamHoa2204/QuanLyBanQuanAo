package server.services;

import java.util.Locale;

import common.enums.LoaiMaTuDong;
import common.enums.VaiTro;
import common.protocol.MaLoi;
import server.entities.TaiKhoan;
import server.utils.QuanLyJpa;

/** SQL sequence cấp mã duy nhất giữa các client và các lần khởi động server. */
public final class DichVuCapMa {
    private final QuanLyJpa jpa;
    private final DichVuXacThuc auth;

    public DichVuCapMa(QuanLyJpa jpa, DichVuXacThuc auth) {
        this.jpa = jpa;
        this.auth = auth;
    }

    public String next(String token, LoaiMaTuDong type) {
        if (type == null) {
            throw new NgoaiLeNghiepVu(MaLoi.INVALID_REQUEST, "Thiếu loại mã.");
        }
        return jpa.transaction(em -> {
            TaiKhoan account = auth.requireAuthenticated(em, token);
            if (type != LoaiMaTuDong.KHACH_HANG && type != LoaiMaTuDong.HOA_DON
                    && account.getVaiTro() != VaiTro.NHAN_VIEN_QUAN_LY) {
                throw new NgoaiLeNghiepVu(MaLoi.FORBIDDEN, "Không có quyền cấp loại mã này.");
            }
            Number value = (Number) em.createNativeQuery("SELECT NEXT VALUE FOR dbo.Seq_" + type.getPrefix())
                    .getSingleResult();
            String code = String.format(Locale.ROOT, "%s%03d", type.getPrefix(), value.longValue());
            if (code.length() > 20) {
                throw new NgoaiLeNghiepVu(MaLoi.VALIDATION_ERROR, "Đã hết phạm vi cấp mã.");
            }
            return code;
        });
    }
}
