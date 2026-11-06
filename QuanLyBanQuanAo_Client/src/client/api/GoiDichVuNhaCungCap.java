package client.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.dto.nhacungcap.DuLieuNhaCungCap;
import common.dto.nhacungcap.KetQuaTraCuuNhaCungCap;
import common.dto.nhacungcap.YeuCauLuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTraCuuNhaCungCap;
import common.dto.nhacungcap.YeuCauTrangThaiNhaCungCap;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

public final class GoiDichVuNhaCungCap {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuNhaCungCap(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public KetQuaTraCuuNhaCungCap search(YeuCauTraCuuNhaCungCap r) throws IOException {
        return call(HanhDong.TRA_CUU_NHA_CUNG_CAP, r, KetQuaTraCuuNhaCungCap.class);
    }

    public DuLieuNhaCungCap create(DuLieuNhaCungCap r) throws IOException {
        return call(HanhDong.THEM_NHA_CUNG_CAP, r, DuLieuNhaCungCap.class);
    }

    public DuLieuNhaCungCap update(YeuCauLuuNhaCungCap r) throws IOException {
        return call(HanhDong.CAP_NHAT_NHA_CUNG_CAP, r, DuLieuNhaCungCap.class);
    }

    public DuLieuNhaCungCap setActive(YeuCauTrangThaiNhaCungCap r, boolean active) throws IOException {
        return call(active ? HanhDong.KICH_HOAT_NHA_CUNG_CAP : HanhDong.NGUNG_HOAT_DONG_NHA_CUNG_CAP, r,
                DuLieuNhaCungCap.class);
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
