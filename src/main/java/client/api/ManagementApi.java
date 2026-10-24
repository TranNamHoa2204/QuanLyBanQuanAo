package client.api;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import client.network.SocketClient;
import client.session.ClientSession;
import common.dto.*;
import common.protocol.*;

/** API các danh mục còn lại; client chỉ gửi DTO, không truy cập SQL/JPA. */
public final class ManagementApi {
    public enum Kind { CUSTOMER, EMPLOYEE, PROMOTION, TAX }
    private final SocketClient client;
    private final ClientSession session;
    public ManagementApi(SocketClient client, ClientSession session) { this.client = client; this.session = session; }
    public ManagementSearchResult search(Kind kind, ManagementSearchRequest r) throws IOException {
        Action action = kind == Kind.CUSTOMER ? Action.TRA_CUU_KHACH_HANG
                : kind == Kind.EMPLOYEE ? Action.TRA_CUU_NHAN_VIEN
                : kind == Kind.TAX ? Action.TRA_CUU_THUE_QUAN_LY : Action.TRA_CUU_KHUYEN_MAI;
        return JsonCodec.mapper().treeToValue(call(action, r), ManagementSearchResult.class);
    }
    public JsonNode save(Kind kind, ManagementSaveRequest r, int mode) throws IOException {
        if (mode < 0 || mode > 2) { throw new IllegalArgumentException("mode"); }
        Action[] actions = kind == Kind.CUSTOMER
                ? new Action[]{Action.THEM_KHACH_HANG, Action.CAP_NHAT_KHACH_HANG, Action.NGUNG_HOAT_DONG_KHACH_HANG}
                : kind == Kind.EMPLOYEE
                ? new Action[]{Action.THEM_NHAN_VIEN, Action.CAP_NHAT_NHAN_VIEN, Action.NGUNG_HOAT_DONG_NHAN_VIEN}
                : kind == Kind.TAX
                ? new Action[]{Action.THEM_THUE, Action.CAP_NHAT_THUE, Action.NGUNG_AP_DUNG_THUE}
                : new Action[]{Action.THEM_KHUYEN_MAI, Action.CAP_NHAT_KHUYEN_MAI, Action.NGUNG_HOAT_DONG_KHUYEN_MAI};
        try { return call(actions[mode], r); }
        finally { r.setInitialPassword(null); }
    }
    public RevenueResult report(RevenueRequest r, boolean products) throws IOException {
        return JsonCodec.mapper().treeToValue(call(products ? Action.THONG_KE_DOANH_THU_THEO_SAN_PHAM
                : Action.THONG_KE_DOANH_THU_THEO_THOI_GIAN, r), RevenueResult.class);
    }
    private JsonNode call(Action action, Object payload) throws IOException {
        Response<JsonNode> response = client.send(action, session.getToken(), payload);
        if (!response.isSuccess()) {
            if (response.getErrorCode() == ErrorCode.UNAUTHENTICATED) { session.clear(); }
            throw new ApiException(response.getErrorCode(), response.getMessage());
        }
        return response.getPayload();
    }
}
