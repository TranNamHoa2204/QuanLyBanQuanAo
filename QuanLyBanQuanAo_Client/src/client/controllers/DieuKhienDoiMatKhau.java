package client.controllers;

import client.api.GoiDichVuXacThuc;

/** View gọi controller qua SwingWorker để không chặn luồng giao diện. */
public final class DieuKhienDoiMatKhau {
    private final GoiDichVuXacThuc api;

    public DieuKhienDoiMatKhau(GoiDichVuXacThuc api) {
        this.api = api;
    }

    public void change(String oldPassword, String newPassword) throws java.io.IOException {
        api.changePassword(oldPassword, newPassword);
    }
}
