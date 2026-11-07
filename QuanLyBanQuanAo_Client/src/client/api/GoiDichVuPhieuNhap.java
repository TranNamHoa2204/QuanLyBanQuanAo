package client.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import client.utils.SinhMaTuDong;
import common.enums.LoaiMaTuDong;
import common.dto.phieunhap.DuLieuDayDuPhieuNhap;
import common.dto.phieunhap.KetQuaTraCuuPhieuNhap;
import common.dto.phieunhap.YeuCauDongPhieuNhap;
import common.dto.phieunhap.YeuCauPhienBanPhieuNhap;
import common.dto.phieunhap.YeuCauTaoPhieuNhap;
import common.dto.phieunhap.YeuCauTraCuuPhieuNhap;
import common.dto.phieunhap.YeuCauXacNhanPhieuNhap;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

public final class GoiDichVuPhieuNhap {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuPhieuNhap(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public KetQuaTraCuuPhieuNhap search(YeuCauTraCuuPhieuNhap r) throws IOException {
        return call(HanhDong.TRA_CUU_PHIEU_NHAP, r, KetQuaTraCuuPhieuNhap.class);
    }

    public DuLieuDayDuPhieuNhap detail(String id) throws IOException {
        YeuCauPhienBanPhieuNhap r = new YeuCauPhienBanPhieuNhap();
        r.setMaPhieuNhap(id);
        return call(HanhDong.XEM_CHI_TIET_PHIEU_NHAP, r, DuLieuDayDuPhieuNhap.class);
    }

    public DuLieuDayDuPhieuNhap create(YeuCauTaoPhieuNhap r) throws IOException {
        if (r != null) {
            r.setMaPhieuNhap(SinhMaTuDong.cap(r.getMaPhieuNhap(), LoaiMaTuDong.PHIEU_NHAP, client, session));
        }
        return call(HanhDong.TAO_PHIEU_NHAP, r, DuLieuDayDuPhieuNhap.class);
    }

    public DuLieuDayDuPhieuNhap addLine(YeuCauDongPhieuNhap r) throws IOException {
        return call(HanhDong.THEM_DONG_PHIEU_NHAP, r, DuLieuDayDuPhieuNhap.class);
    }

    public DuLieuDayDuPhieuNhap updateLine(YeuCauDongPhieuNhap r) throws IOException {
        return call(HanhDong.CAP_NHAT_DONG_PHIEU_NHAP, r, DuLieuDayDuPhieuNhap.class);
    }

    public DuLieuDayDuPhieuNhap removeLine(YeuCauDongPhieuNhap r) throws IOException {
        return call(HanhDong.BO_DONG_PHIEU_NHAP, r, DuLieuDayDuPhieuNhap.class);
    }

    public DuLieuDayDuPhieuNhap confirm(YeuCauXacNhanPhieuNhap r) throws IOException {
        return call(HanhDong.XAC_NHAN_PHIEU_NHAP, r, DuLieuDayDuPhieuNhap.class);
    }

    public DuLieuDayDuPhieuNhap cancel(YeuCauPhienBanPhieuNhap r) throws IOException {
        return call(HanhDong.HUY_PHIEU_NHAP, r, DuLieuDayDuPhieuNhap.class);
    }

    private <T> T call(HanhDong action, Object payload, Class<T> type) throws IOException {
        PhanHoi<JsonNode> response = client.send(action, session.getToken(), payload);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == MaLoi.UNAUTHENTICATED) {
                session.clear();
            }
            throw new NgoaiLeGoiDichVu(response.getErrorCode(), response.getMessage());
        }
        return BoMaHoaJson.mapper().treeToValue(response.getPayload(), type);
    }
}
