package client.api;

import common.protocol.ErrorCode;

public final class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final ErrorCode code;

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
