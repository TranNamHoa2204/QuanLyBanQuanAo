package server.service;

import common.protocol.ErrorCode;

public final class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final ErrorCode code;
    public BusinessException(ErrorCode code, String message) { super(message); this.code = code; }
    public ErrorCode getCode() { return code; }
}
