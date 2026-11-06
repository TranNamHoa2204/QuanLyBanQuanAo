package client.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.nhanvien.DuLieuNhanVien;
import common.dto.taikhoan.KetQuaDangNhap;
import common.dto.taikhoan.YeuCauDangNhap;
import common.dto.taikhoan.YeuCauDoiMatKhau;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

public final class GoiDichVuXacThuc {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuXacThuc(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public GoiDichVuSanPham products() {
        return new GoiDichVuSanPham(client, session);
    }

    public GoiDichVuHoaDon invoices() {
        return new GoiDichVuHoaDon(client, session);
    }

    public GoiDichVuLichSuKho stockHistory() {
        return new GoiDichVuLichSuKho(client, session);
    }

    public GoiDichVuNhanVien employees() {
        return new GoiDichVuNhanVien(client, session);
    }

    public GoiDichVuKhachHang customers() {
        return new GoiDichVuKhachHang(client, session);
    }

    public GoiDichVuKhuyenMai promotions() {
        return new GoiDichVuKhuyenMai(client, session);
    }

    public GoiDichVuThongKe statistics() {
        return new GoiDichVuThongKe(client, session);
    }

    public GoiDichVuNhaCungCap suppliers() {
        return new GoiDichVuNhaCungCap(client, session);
    }

    public GoiDichVuPhieuNhap receipts() {
        return new GoiDichVuPhieuNhap(client, session);
    }

    public KetQuaDangNhap login(String username, String password) throws IOException {
        YeuCauDangNhap request = new YeuCauDangNhap();
        request.setUsername(username);
        request.setPassword(password);
        session.clear();
        try {
            KetQuaDangNhap result = call(HanhDong.DANG_NHAP, request, KetQuaDangNhap.class);
            session.setLogin(result);
            return result;
        } finally {
            request.setPassword(null);
        }
    }

    public DuLieuNhanVien profile() throws IOException {
        return call(HanhDong.XEM_THONG_TIN_CA_NHAN, null, DuLieuNhanVien.class);
    }

    public void changePassword(String oldPassword, String newPassword) throws IOException {
        YeuCauDoiMatKhau request = new YeuCauDoiMatKhau();
        request.setMatKhauCu(oldPassword);
        request.setMatKhauMoi(newPassword);
        try {
            call(HanhDong.DOI_MAT_KHAU, request, Void.class);
            session.clear();
        } finally {
            request.setMatKhauCu(null);
            request.setMatKhauMoi(null);
        }
    }

    public void logout() throws IOException {
        try {
            call(HanhDong.DANG_XUAT, null, Void.class);
        } catch (NgoaiLeGoiDichVu failure) {
            if (failure.getCode() != MaLoi.UNAUTHENTICATED) {
                throw failure;
            }
        } finally {
            session.clear();
            client.close();
        }
    }

    private <T> T call(HanhDong action, Object payload, Class<T> type) throws IOException {
        PhanHoi<JsonNode> response = client.send(action, session.getToken(), payload);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == MaLoi.UNAUTHENTICATED) {
                session.clear();
            }
            throw new NgoaiLeGoiDichVu(response.getErrorCode(), response.getMessage());
        }
        return type == Void.class ? null : BoMaHoaJson.mapper().treeToValue(response.getPayload(), type);
    }
}
