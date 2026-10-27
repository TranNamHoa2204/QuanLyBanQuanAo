package client.api;

import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import client.network.SocketClient;
import client.session.ClientSession;
import common.dto.*;
import common.protocol.*;

public final class HoaDonApi {
    private final SocketClient client;
    private final ClientSession session;

    public HoaDonApi(SocketClient client, ClientSession session) {
        this.client = client;
        this.session = session;
    }

    public HoaDonDetailDTO create(HoaDonCreateRequest r) throws IOException {
        return invoice(Action.LAP_HOA_DON, r);
    }

    public HoaDonDetailDTO detail(String id) throws IOException {
        HoaDonVersionRequest r = new HoaDonVersionRequest();
        r.setMaHD(id);
        return invoice(Action.XEM_CHI_TIET_HOA_DON, r);
    }

    public HoaDonDetailDTO line(HoaDonLineRequest r, int mode) throws IOException {
        return invoice(
                mode == 0 ? Action.THEM_CHI_TIET_HOA_DON : mode == 1 ? Action.CAP_NHAT_SO_LUONG : Action.BO_MAT_HANG,
                r);
    }

    public HoaDonDetailDTO pay(HoaDonVersionRequest r) throws IOException {
        return invoice(Action.THANH_TOAN_HOA_DON, r);
    }

    public HoaDonDetailDTO cancel(HoaDonVersionRequest r) throws IOException {
        return invoice(Action.HUY_HOA_DON_NHAP, r);
    }

    public HoaDonSearchResult search(HoaDonSearchRequest r) throws IOException {
        return call(Action.TRA_CUU_HOA_DON, r, JsonCodec.mapper().constructType(HoaDonSearchResult.class));
    }

    public List<ThueDTO> taxes() throws IOException {
        return call(Action.TRA_CUU_THUE, null,
                JsonCodec.mapper().getTypeFactory().constructCollectionType(List.class, ThueDTO.class));
    }

    private HoaDonDetailDTO invoice(Action a, Object r) throws IOException {
        return call(a, r, JsonCodec.mapper().constructType(HoaDonDetailDTO.class));
    }

    private <T> T call(Action a, Object r, JavaType type) throws IOException {
        Response<JsonNode> response = client.send(a, session.getToken(), r);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == ErrorCode.UNAUTHENTICATED) {
                session.clear();
            }
            throw new ApiException(response.getErrorCode(), response.getMessage());
        }
        return JsonCodec.mapper().convertValue(response.getPayload(), type);
    }
}
