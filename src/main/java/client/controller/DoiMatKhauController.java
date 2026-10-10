package client.controller;

import client.api.AuthApi;

/** View gọi controller qua SwingWorker để không chặn luồng giao diện. */
public final class DoiMatKhauController {
    private final AuthApi api;
    public DoiMatKhauController(AuthApi api) { this.api = api; }
    public void change(String oldPassword, String newPassword) throws java.io.IOException {
        api.changePassword(oldPassword, newPassword);
    }
}
