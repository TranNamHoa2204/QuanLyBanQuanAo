package client.controllers;

import client.api.GoiDichVuXacThuc;
import common.dto.nhanvien.DuLieuNhanVien;

/** View gọi controller qua SwingWorker để không chặn luồng giao diện. */
public final class DieuKhienThongTinCaNhan {
    private final GoiDichVuXacThuc api;

    public DieuKhienThongTinCaNhan(GoiDichVuXacThuc api) {
        this.api = api;
    }

    public DuLieuNhanVien load() throws java.io.IOException {
        return api.profile();
    }
}
