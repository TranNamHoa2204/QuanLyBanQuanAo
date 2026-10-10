package common.protocol;

/** Yêu cầu JSON trong frame TCP: int32 độ dài + byte JSON UTF-8. */
public class Request<T> {
    private String requestId;
    private Action action;
    private String sessionToken;
    private T payload;

    public Request() { }
    public Request(String requestId, Action action, String sessionToken, T payload) {
        this.requestId = requestId;
        this.action = action;
        this.sessionToken = sessionToken;
        this.payload = payload;
    }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }
}
