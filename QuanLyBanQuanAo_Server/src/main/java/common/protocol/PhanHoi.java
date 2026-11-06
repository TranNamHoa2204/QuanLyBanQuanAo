package common.protocol;

/** Không chứa exception nội bộ, mật khẩu hoặc entity JPA. */
public class PhanHoi<T> {
    private String requestId;
    private boolean success;
    private MaLoi errorCode = MaLoi.NONE;
    private String message;
    private T payload;

    public PhanHoi() {
    }

    public static <T> PhanHoi<T> success(String requestId, T payload) {
        PhanHoi<T> response = new PhanHoi<T>();
        response.requestId = requestId;
        response.success = true;
        response.payload = payload;
        return response;
    }

    public static <T> PhanHoi<T> failure(String requestId, MaLoi code, String message) {
        if (code == null || code == MaLoi.NONE) {
            throw new IllegalArgumentException("Failure requires an error code.");
        }
        PhanHoi<T> response = new PhanHoi<T>();
        response.requestId = requestId;
        response.errorCode = code;
        response.message = message;
        return response;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public MaLoi getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(MaLoi errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
