package server.services;

import common.protocol.MaLoi;

public final class NgoaiLeNghiepVu extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final MaLoi code;

    public NgoaiLeNghiepVu(MaLoi code, String message) {
        super(message);
        this.code = code;
    }

    public MaLoi getCode() {
        return code;
    }
}
