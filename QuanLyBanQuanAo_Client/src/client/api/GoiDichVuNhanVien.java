package client.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.nhanvien.DuLieuHoSoNhanVien;
import common.dto.nhanvien.KetQuaTraCuuNhanVien;
import common.dto.nhanvien.YeuCauChiTietNhanVien;
import common.dto.nhanvien.YeuCauLuuNhanVien;
import common.dto.nhanvien.YeuCauTaoNhanVien;
import common.dto.nhanvien.YeuCauTraCuuNhanVien;
import common.dto.nhanvien.YeuCauTrangThaiNhanVien;
import common.dto.taikhoan.DuLieuTaiKhoan;
import common.dto.taikhoan.YeuCauTaoTaiKhoan;
import common.dto.taikhoan.YeuCauTrangThaiTaiKhoan;
import common.dto.taikhoan.YeuCauVaiTroTaiKhoan;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

/** Client API for manager-only employee and account operations. */
public final class GoiDichVuNhanVien {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuNhanVien(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public KetQuaTraCuuNhanVien search(YeuCauTraCuuNhanVien request) throws IOException {
        return call(HanhDong.TRA_CUU_NHAN_VIEN, request, KetQuaTraCuuNhanVien.class);
    }

    public DuLieuHoSoNhanVien detail(String maNV) throws IOException {
        YeuCauChiTietNhanVien request = new YeuCauChiTietNhanVien();
        request.setMaNV(maNV);
        return call(HanhDong.XEM_CHI_TIET_NHAN_VIEN, request, DuLieuHoSoNhanVien.class);
    }

    public DuLieuHoSoNhanVien create(YeuCauTaoNhanVien request) throws IOException {
        try {
            return call(HanhDong.THEM_NHAN_VIEN, request, DuLieuHoSoNhanVien.class);
        } finally {
            if (request != null && request.getTaiKhoan() != null) {
                request.getTaiKhoan().setMatKhauBanDau(null);
            }
        }
    }

    public DuLieuHoSoNhanVien update(YeuCauLuuNhanVien request) throws IOException {
        return call(HanhDong.CAP_NHAT_NHAN_VIEN, request, DuLieuHoSoNhanVien.class);
    }

    public DuLieuHoSoNhanVien setEmployeeActive(YeuCauTrangThaiNhanVien request, boolean active)
            throws IOException {
        return call(active ? HanhDong.KICH_HOAT_NHAN_VIEN : HanhDong.NGUNG_HOAT_DONG_NHAN_VIEN, request,
                DuLieuHoSoNhanVien.class);
    }

    public DuLieuTaiKhoan createAccount(YeuCauTaoTaiKhoan request) throws IOException {
        try {
            return call(HanhDong.TAO_TAI_KHOAN, request, DuLieuTaiKhoan.class);
        } finally {
            if (request != null) {
                request.setMatKhauBanDau(null);
            }
        }
    }

    public DuLieuTaiKhoan setAccountActive(YeuCauTrangThaiTaiKhoan request, boolean active)
            throws IOException {
        return call(active ? HanhDong.MO_KHOA_TAI_KHOAN : HanhDong.KHOA_TAI_KHOAN, request,
                DuLieuTaiKhoan.class);
    }

    public DuLieuTaiKhoan changeRole(YeuCauVaiTroTaiKhoan request) throws IOException {
        return call(HanhDong.CAP_NHAT_VAI_TRO_TAI_KHOAN, request, DuLieuTaiKhoan.class);
    }

    private <T> T call(HanhDong action, Object payload, Class<T> type) throws IOException {
        PhanHoi<JsonNode> response = client.send(action, session.getToken(), payload);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == MaLoi.UNAUTHENTICATED) {
                session.clear();
            }
            throw new NgoaiLeGoiDichVu(response.getErrorCode(), response.getMessage());
        }
        return response.getPayload() == null ? null
                : BoMaHoaJson.mapper().treeToValue(response.getPayload(), type);
    }
}
