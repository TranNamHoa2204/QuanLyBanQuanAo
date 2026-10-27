package client.controller;

import client.api.AuthApi;

/** View gọi controller qua SwingWorker để không chặn luồng giao diện. */
public final class DangNhapController {
    private final AuthApi api;

    public DangNhapController(AuthApi api) {
        this.api = api;
    }

    public common.dto.LoginResponse login(String username, String password) throws java.io.IOException {
        return api.login(username, password);
    }
}
