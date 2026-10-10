package common.protocol;

/** Không chứa exception nội bộ, mật khẩu hoặc entity JPA. */
public class Response<T> {
    private String requestId;
    private boolean success;
    private ErrorCode errorCode = ErrorCode.NONE;
    private String message;
    private T payload;

    public Response() { }
    public static <T> Response<T> success(String requestId, T payload) {
        Response<T> response = new Response<T>();
        response.requestId = requestId;
        response.success = true;
        response.payload = payload;
        return response;
    }
    public static <T> Response<T> failure(String requestId, ErrorCode code, String message) {
        if (code == null || code == ErrorCode.NONE) {
            throw new IllegalArgumentException("Failure requires an error code.");
        }
        Response<T> response = new Response<T>();
        response.requestId = requestId;
        response.errorCode = code;
        response.message = message;
        return response;
    }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public ErrorCode getErrorCode() { return errorCode; }
    public void setErrorCode(ErrorCode errorCode) { this.errorCode = errorCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }
}
