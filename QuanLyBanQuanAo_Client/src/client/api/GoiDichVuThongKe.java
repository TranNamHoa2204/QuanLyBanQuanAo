package client.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.thongke.DuLieuThongKeDoanhThu;
import common.dto.thongke.YeuCauThongKeDoanhThu;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

/** Manager-only statistics API; report calculations are performed by the server. */
public final class GoiDichVuThongKe {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuThongKe(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public DuLieuThongKeDoanhThu byTime(YeuCauThongKeDoanhThu request) throws IOException {
        return call(HanhDong.THONG_KE_DOANH_THU_THEO_THOI_GIAN, request);
    }

    public DuLieuThongKeDoanhThu byProduct(YeuCauThongKeDoanhThu request) throws IOException {
        return call(HanhDong.THONG_KE_DOANH_THU_THEO_SAN_PHAM, request);
    }

    private DuLieuThongKeDoanhThu call(HanhDong action, YeuCauThongKeDoanhThu request) throws IOException {
        PhanHoi<JsonNode> response = client.send(action, session.getToken(), request);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == MaLoi.UNAUTHENTICATED) {
                session.clear();
            }
            throw new NgoaiLeGoiDichVu(response.getErrorCode(), response.getMessage());
        }
        return BoMaHoaJson.mapper().treeToValue(response.getPayload(), DuLieuThongKeDoanhThu.class);
    }
}
