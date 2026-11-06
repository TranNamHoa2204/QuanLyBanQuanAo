package client.api;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.danhmuc.DuLieuThue;
import common.dto.hoadon.DuLieuDayDuHoaDon;
import common.dto.hoadon.KetQuaTraCuuHoaDon;
import common.dto.hoadon.YeuCauDongHoaDon;
import common.dto.hoadon.YeuCauGanKhachHangHoaDon;
import common.dto.hoadon.YeuCauPhienBanHoaDon;
import common.dto.hoadon.YeuCauTaoHoaDon;
import common.dto.hoadon.YeuCauThanhToanHoaDon;
import common.dto.hoadon.YeuCauTraCuuHoaDon;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

public final class GoiDichVuHoaDon {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuHoaDon(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public DuLieuDayDuHoaDon create(YeuCauTaoHoaDon r) throws IOException {
        return invoice(HanhDong.LAP_HOA_DON, r);
    }

    public DuLieuDayDuHoaDon detail(String id) throws IOException {
        YeuCauPhienBanHoaDon r = new YeuCauPhienBanHoaDon();
        r.setMaHD(id);
        return invoice(HanhDong.XEM_CHI_TIET_HOA_DON, r);
    }

    public DuLieuDayDuHoaDon line(YeuCauDongHoaDon r, int mode) throws IOException {
        return invoice(mode == 0 ? HanhDong.THEM_CHI_TIET_HOA_DON
                : mode == 1 ? HanhDong.CAP_NHAT_SO_LUONG : HanhDong.BO_MAT_HANG, r);
    }

    public DuLieuDayDuHoaDon setCustomer(YeuCauGanKhachHangHoaDon r) throws IOException {
        return invoice(HanhDong.CAP_NHAT_KHACH_HANG_HOA_DON_NHAP, r);
    }

    public DuLieuDayDuHoaDon pay(YeuCauThanhToanHoaDon r) throws IOException {
        return invoice(HanhDong.THANH_TOAN_HOA_DON, r);
    }

    public DuLieuDayDuHoaDon cancel(YeuCauPhienBanHoaDon r) throws IOException {
        return invoice(HanhDong.HUY_HOA_DON_NHAP, r);
    }

    public KetQuaTraCuuHoaDon search(YeuCauTraCuuHoaDon r) throws IOException {
        return call(HanhDong.TRA_CUU_HOA_DON, r,
                BoMaHoaJson.mapper().constructType(KetQuaTraCuuHoaDon.class));
    }

    public List<DuLieuThue> taxes() throws IOException {
        return call(HanhDong.TRA_CUU_THUE, null,
                BoMaHoaJson.mapper().getTypeFactory().constructCollectionType(List.class, DuLieuThue.class));
    }

    private DuLieuDayDuHoaDon invoice(HanhDong a, Object r) throws IOException {
        return call(a, r, BoMaHoaJson.mapper().constructType(DuLieuDayDuHoaDon.class));
    }

    private <T> T call(HanhDong a, Object r, JavaType type) throws IOException {
        PhanHoi<JsonNode> response = client.send(a, session.getToken(), r);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == MaLoi.UNAUTHENTICATED) {
                session.clear();
            }
            throw new NgoaiLeGoiDichVu(response.getErrorCode(), response.getMessage());
        }
        return BoMaHoaJson.mapper().convertValue(response.getPayload(), type);
    }
}
