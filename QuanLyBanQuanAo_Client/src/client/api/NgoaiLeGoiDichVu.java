package client.api;

import common.protocol.MaLoi;

public final class NgoaiLeGoiDichVu extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final MaLoi code;

    public NgoaiLeGoiDichVu(MaLoi code, String message) {
        super(message);
        this.code = code;
    }

    public MaLoi getCode() {
        return code;
    }
}
