package client.api;

import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import client.network.SocketClient;
import client.session.ClientSession;
import common.dto.*;
import common.protocol.*;

public final class SanPhamApi {
    private final SocketClient client;
    private final ClientSession session;

    public SanPhamApi(SocketClient client, ClientSession session) {
        this.client = client;
        this.session = session;
    }

    public String saveProduct(SanPhamSaveRequest r, int mode) throws IOException {
        return call(
                mode == 0 ? Action.THEM_SAN_PHAM
                        : mode == 1 ? Action.CAP_NHAT_SAN_PHAM : Action.NGUNG_KINH_DOANH_SAN_PHAM,
                r, JsonCodec.mapper().constructType(String.class));
    }

    public String saveCategory(DanhMucSaveRequest r, boolean create) throws IOException {
        return call(create ? Action.THEM_LOAI_SAN_PHAM : Action.CAP_NHAT_LOAI_SAN_PHAM, r,
                JsonCodec.mapper().constructType(String.class));
    }

    public String saveBrand(DanhMucSaveRequest r, boolean create) throws IOException {
        return call(create ? Action.THEM_THUONG_HIEU : Action.CAP_NHAT_THUONG_HIEU, r,
                JsonCodec.mapper().constructType(String.class));
    }

    public BienTheDTO saveVariant(BienTheDTO r, boolean create) throws IOException {
        return call(create ? Action.THEM_BIEN_THE : Action.CAP_NHAT_BIEN_THE, r,
                JsonCodec.mapper().constructType(BienTheDTO.class));
    }

    public SanPhamSearchResult search(SanPhamSearchRequest request) throws IOException {
        return call(Action.TRA_CUU_SAN_PHAM, request, JsonCodec.mapper().constructType(SanPhamSearchResult.class));
    }

    public SanPhamDetailDTO detail(String maSP) throws IOException {
        SanPhamDetailRequest request = new SanPhamDetailRequest();
        request.setMaSP(maSP);
        return call(Action.XEM_CHI_TIET_SAN_PHAM, request, JsonCodec.mapper().constructType(SanPhamDetailDTO.class));
    }

    public List<LoaiSanPhamDTO> categories() throws IOException {
        return call(Action.TRA_CUU_LOAI_SAN_PHAM, null,
                JsonCodec.mapper().getTypeFactory().constructCollectionType(List.class, LoaiSanPhamDTO.class));
    }

    public List<ThuongHieuDTO> brands() throws IOException {
        return call(Action.TRA_CUU_THUONG_HIEU, null,
                JsonCodec.mapper().getTypeFactory().constructCollectionType(List.class, ThuongHieuDTO.class));
    }

    private <T> T call(Action action, Object payload, JavaType type) throws IOException {
        Response<JsonNode> response = client.send(action, session.getToken(), payload);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == ErrorCode.UNAUTHENTICATED) {
                session.clear();
            }
            throw new ApiException(response.getErrorCode(), response.getMessage());
        }
        return JsonCodec.mapper().convertValue(response.getPayload(), type);
    }
}
