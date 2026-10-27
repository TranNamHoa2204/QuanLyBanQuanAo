package client.api;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import common.dto.*;
import client.network.SocketClient;
import client.session.ClientSession;

/** API quản lý cấu hình thuế qua cùng giao thức Socket. */
public final class ThueApi {
    private final ManagementApi api;

    public ThueApi(SocketClient client, ClientSession session) {
        api = new ManagementApi(client, session);
    }

    public ManagementSearchResult search(ManagementSearchRequest request) throws IOException {
        return api.search(ManagementApi.Kind.TAX, request);
    }

    public JsonNode save(ManagementSaveRequest request, int mode) throws IOException {
        return api.save(ManagementApi.Kind.TAX, request, mode);
    }
}
