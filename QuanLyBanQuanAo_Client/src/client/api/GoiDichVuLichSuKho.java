package client.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.kho.DuLieuLichSuKho;
import common.dto.kho.KetQuaTraCuuLichSuKho;
import common.dto.kho.YeuCauDieuChinhTonKho;
import common.dto.kho.YeuCauTraCuuLichSuKho;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

/** Socket client cho lịch sử kho và điều chỉnh tồn có version. */
public final class GoiDichVuLichSuKho {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuLichSuKho(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public KetQuaTraCuuLichSuKho search(YeuCauTraCuuLichSuKho request) throws IOException {
        return call(HanhDong.TRA_CUU_LICH_SU_KHO, request, KetQuaTraCuuLichSuKho.class);
    }

    public DuLieuLichSuKho adjust(YeuCauDieuChinhTonKho request) throws IOException {
        return call(HanhDong.DIEU_CHINH_TON_KHO, request, DuLieuLichSuKho.class);
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
