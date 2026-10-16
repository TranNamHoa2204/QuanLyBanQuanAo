package client.api;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import common.dto.*;
import common.protocol.*;
import client.network.SocketClient;
import client.session.ClientSession;

public final class AuthApi {
    private final SocketClient client;
    private final ClientSession session;
    public AuthApi(SocketClient client, ClientSession session) { this.client = client; this.session = session; }
    public SanPhamApi products() { return new SanPhamApi(client, session); }
    public HoaDonApi invoices() { return new HoaDonApi(client, session); }
    public LichSuKhoApi stockHistory() { return new LichSuKhoApi(client, session); }
    public LoginResponse login(String username, String password) throws IOException {
        LoginRequest request = new LoginRequest();
        request.setUsername(username); request.setPassword(password);
        session.clear();
        try {
            LoginResponse result = call(Action.DANG_NHAP, request, LoginResponse.class);
            session.setLogin(result); return result;
        } finally { request.setPassword(null); }
    }
    public NhanVienDTO profile() throws IOException { return call(Action.XEM_THONG_TIN_CA_NHAN, null, NhanVienDTO.class); }
    public void changePassword(String oldPassword, String newPassword) throws IOException {
        DoiMatKhauRequest request = new DoiMatKhauRequest();
        request.setMatKhauCu(oldPassword); request.setMatKhauMoi(newPassword);
        try { call(Action.DOI_MAT_KHAU, request, Void.class); session.clear(); }
        finally { request.setMatKhauCu(null); request.setMatKhauMoi(null); }
    }
    public void logout() throws IOException {
        try { call(Action.DANG_XUAT, null, Void.class); }
        catch (ApiException failure) {
            if (failure.getCode() != ErrorCode.UNAUTHENTICATED) { throw failure; }
        }
        finally { session.clear(); client.close(); }
    }
    private <T> T call(Action action, Object payload, Class<T> type) throws IOException {
        Response<JsonNode> response = client.send(action, session.getToken(), payload);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == ErrorCode.UNAUTHENTICATED) { session.clear(); }
            throw new ApiException(response.getErrorCode(), response.getMessage());
        }
        return type == Void.class ? null : JsonCodec.mapper().treeToValue(response.getPayload(), type);
    }
}
