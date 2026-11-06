package common.protocol;

/** Yêu cầu JSON trong frame TCP: int32 độ dài + byte JSON UTF-8. */
public class YeuCau<T> {
    private String requestId;
    private HanhDong action;
    private String sessionToken;
    private T payload;

    public YeuCau() {
    }

    public YeuCau(String requestId, HanhDong action, String sessionToken, T payload) {
        this.requestId = requestId;
        this.action = action;
        this.sessionToken = sessionToken;
        this.payload = payload;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public HanhDong getAction() {
        return action;
    }

    public void setAction(HanhDong action) {
        this.action = action;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
