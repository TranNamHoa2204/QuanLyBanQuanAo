package client.api;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import common.dto.DieuChinhTonKhoRequest;
import common.dto.LichSuKhoDTO;
import common.dto.LichSuKhoSearchRequest;
import common.dto.LichSuKhoSearchResult;
import common.protocol.Action;
import common.protocol.ErrorCode;
import common.protocol.JsonCodec;
import common.protocol.Response;
import client.network.SocketClient;
import client.session.ClientSession;

/** Socket client cho lịch sử kho và điều chỉnh tồn có version. */
public final class LichSuKhoApi {
    private final SocketClient client;
    private final ClientSession session;

    public LichSuKhoApi(SocketClient client, ClientSession session) {
        this.client = client;
        this.session = session;
    }

    public LichSuKhoSearchResult search(LichSuKhoSearchRequest request) throws IOException {
        return call(Action.TRA_CUU_LICH_SU_KHO, request, LichSuKhoSearchResult.class);
    }

    public LichSuKhoDTO adjust(DieuChinhTonKhoRequest request) throws IOException {
        return call(Action.DIEU_CHINH_TON_KHO, request, LichSuKhoDTO.class);
    }

    private <T> T call(Action action, Object payload, Class<T> type) throws IOException {
        Response<JsonNode> response = client.send(action, session.getToken(), payload);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == ErrorCode.UNAUTHENTICATED) { session.clear(); }
            throw new ApiException(response.getErrorCode(), response.getMessage());
        }
        return JsonCodec.mapper().treeToValue(response.getPayload(), type);
    }
}
