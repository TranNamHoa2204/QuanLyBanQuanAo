package client.utils;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import client.api.NgoaiLeGoiDichVu;
import client.network.KetNoiMayChu;
import client.session.PhienDangNhap;
import common.enums.LoaiMaTuDong;
import common.dto.danhmuc.YeuCauCapMa;
import common.protocol.HanhDong;
import common.protocol.MaLoi;
import common.protocol.PhanHoi;

/** Được gọi trong tác vụ nền trước khi gửi yêu cầu tạo bản ghi. */
public final class SinhMaTuDong {
    private SinhMaTuDong() {
    }

    public static String cap(String current, LoaiMaTuDong type, KetNoiMayChu client, PhienDangNhap session)
            throws IOException {
        if (current != null && !current.trim().isEmpty()) {
            return current;
        }
        YeuCauCapMa request = new YeuCauCapMa();
        request.setLoai(type);
        PhanHoi<JsonNode> response = client.send(HanhDong.CAP_MA_TU_DONG, session.getToken(), request);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == MaLoi.UNAUTHENTICATED) {
                session.clear();
            }
            throw new NgoaiLeGoiDichVu(response.getErrorCode(), response.getMessage());
        }
        return response.getPayload().asText();
    }
}
