package client.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import client.utils.SinhMaTuDong;
import common.enums.LoaiMaTuDong;
import common.dto.khuyenmai.DuLieuKhuyenMai;
import common.dto.khuyenmai.KetQuaTraCuuKhuyenMai;
import common.dto.khuyenmai.YeuCauChiTietKhuyenMai;
import common.dto.khuyenmai.YeuCauLuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTraCuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTrangThaiKhuyenMai;
import common.protocol.BoMaHoaJson;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

/** Socket API for manager promotion maintenance and active promotion lookup. */
public final class GoiDichVuKhuyenMai {
    private final KetNoiMayChu client;
    private final PhienDangNhap session;

    public GoiDichVuKhuyenMai(KetNoiMayChu client, PhienDangNhap session) {
        this.client = client;
        this.session = session;
    }

    public KetQuaTraCuuKhuyenMai search(YeuCauTraCuuKhuyenMai request) throws IOException {
        return call(HanhDong.TRA_CUU_KHUYEN_MAI, request, KetQuaTraCuuKhuyenMai.class);
    }

    public DuLieuKhuyenMai detail(String id) throws IOException {
        YeuCauChiTietKhuyenMai request = new YeuCauChiTietKhuyenMai();
        request.setMaKM(id);
        return call(HanhDong.XEM_CHI_TIET_KHUYEN_MAI, request, DuLieuKhuyenMai.class);
    }

    public DuLieuKhuyenMai create(DuLieuKhuyenMai request) throws IOException {
        if (request != null) {
            request.setMaKM(SinhMaTuDong.cap(request.getMaKM(), LoaiMaTuDong.KHUYEN_MAI, client, session));
        }
        return call(HanhDong.THEM_KHUYEN_MAI, request, DuLieuKhuyenMai.class);
    }

    public DuLieuKhuyenMai update(YeuCauLuuKhuyenMai request) throws IOException {
        return call(HanhDong.CAP_NHAT_KHUYEN_MAI, request, DuLieuKhuyenMai.class);
    }

    public DuLieuKhuyenMai setActive(YeuCauTrangThaiKhuyenMai request, boolean active) throws IOException {
        return call(active ? HanhDong.KICH_HOAT_KHUYEN_MAI : HanhDong.NGUNG_HOAT_DONG_KHUYEN_MAI, request,
                DuLieuKhuyenMai.class);
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
