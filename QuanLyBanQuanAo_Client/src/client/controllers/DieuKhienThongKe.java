package client.controllers;

import java.io.IOException;

import client.api.GoiDichVuThongKe;
import common.dto.thongke.DuLieuThongKeDoanhThu;
import common.dto.thongke.YeuCauThongKeDoanhThu;

public final class DieuKhienThongKe {
    private final GoiDichVuThongKe api;

    public DieuKhienThongKe(GoiDichVuThongKe api) {
        this.api = api;
    }

    public DuLieuThongKeDoanhThu byTime(YeuCauThongKeDoanhThu request) throws IOException {
        return api.byTime(request);
    }

    public DuLieuThongKeDoanhThu byProduct(YeuCauThongKeDoanhThu request) throws IOException {
        return api.byProduct(request);
    }
}
