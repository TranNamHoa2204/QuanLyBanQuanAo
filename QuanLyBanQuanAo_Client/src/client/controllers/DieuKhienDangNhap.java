package client.controllers;

import client.api.GoiDichVuXacThuc;
import common.dto.taikhoan.KetQuaDangNhap;

/** View gọi controller qua SwingWorker để không chặn luồng giao diện. */
public final class DieuKhienDangNhap {
    private final GoiDichVuXacThuc api;

    public DieuKhienDangNhap(GoiDichVuXacThuc api) {
        this.api = api;
    }

    public KetQuaDangNhap login(String username, String password) throws java.io.IOException {
        return api.login(username, password);
    }
}
