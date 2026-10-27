package client.controller;

import client.api.AuthApi;

/** View gọi controller qua SwingWorker để không chặn luồng giao diện. */
public final class ThongTinCaNhanController {
    private final AuthApi api;

    public ThongTinCaNhanController(AuthApi api) {
        this.api = api;
    }

    public common.dto.NhanVienDTO load() throws java.io.IOException {
        return api.profile();
    }
}
