package client.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.khachhang.DuLieuKhachHang;
import common.dto.khachhang.KetQuaTraCuuKhachHang;
import common.dto.khachhang.YeuCauChiTietKhachHang;
import common.dto.khachhang.YeuCauLuuKhachHang;
import common.dto.khachhang.YeuCauTraCuuKhachHang;
import common.dto.khachhang.YeuCauTrangThaiKhachHang;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

/** Socket API for customer lookup and profile maintenance. */
public final class GoiDichVuKhachHang {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuKhachHang(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public KetQuaTraCuuKhachHang search(YeuCauTraCuuKhachHang request) throws IOException {
        return call(HanhDong.TRA_CUU_KHACH_HANG, request, KetQuaTraCuuKhachHang.class);
    }

    public DuLieuKhachHang detail(String id) throws IOException {
        YeuCauChiTietKhachHang request = new YeuCauChiTietKhachHang();
        request.setMaKH(id);
        return call(HanhDong.XEM_CHI_TIET_KHACH_HANG, request, DuLieuKhachHang.class);
    }

    public DuLieuKhachHang create(DuLieuKhachHang request) throws IOException {
        return call(HanhDong.THEM_KHACH_HANG, request, DuLieuKhachHang.class);
    }

    public DuLieuKhachHang update(YeuCauLuuKhachHang request) throws IOException {
        return call(HanhDong.CAP_NHAT_KHACH_HANG, request, DuLieuKhachHang.class);
    }

    public DuLieuKhachHang setActive(YeuCauTrangThaiKhachHang request, boolean active) throws IOException {
        return call(active ? HanhDong.KICH_HOAT_KHACH_HANG : HanhDong.NGUNG_HOAT_DONG_KHACH_HANG, request,
                DuLieuKhachHang.class);
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
