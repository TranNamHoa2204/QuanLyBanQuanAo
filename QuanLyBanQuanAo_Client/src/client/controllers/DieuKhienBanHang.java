package client.controllers;

import java.io.IOException;
import java.util.List;

import client.api.GoiDichVuHoaDon;
import common.dto.danhmuc.DuLieuThue;
import common.dto.hoadon.DuLieuDayDuHoaDon;
import common.dto.hoadon.KetQuaTraCuuHoaDon;
import common.dto.hoadon.YeuCauDongHoaDon;
import common.dto.hoadon.YeuCauGanKhachHangHoaDon;
import common.dto.hoadon.YeuCauPhienBanHoaDon;
import common.dto.hoadon.YeuCauTaoHoaDon;
import common.dto.hoadon.YeuCauThanhToanHoaDon;
import common.dto.hoadon.YeuCauTraCuuHoaDon;

/** Điều phối bán hàng; SwingWorker do view quản lý, nghiệp vụ tin cậy nằm trên server. */
public final class DieuKhienBanHang {
    private final GoiDichVuHoaDon api;

    public DieuKhienBanHang(GoiDichVuHoaDon api) {
        this.api = api;
    }

    public DuLieuDayDuHoaDon create(YeuCauTaoHoaDon r) throws IOException {
        return api.create(r);
    }

    public DuLieuDayDuHoaDon detail(String id) throws IOException {
        return api.detail(id);
    }

    public DuLieuDayDuHoaDon line(YeuCauDongHoaDon r, int mode) throws IOException {
        return api.line(r, mode);
    }

    public DuLieuDayDuHoaDon setCustomer(YeuCauGanKhachHangHoaDon r) throws IOException {
        return api.setCustomer(r);
    }

    public DuLieuDayDuHoaDon pay(YeuCauThanhToanHoaDon r) throws IOException {
        return api.pay(r);
    }

    public DuLieuDayDuHoaDon cancel(YeuCauPhienBanHoaDon r) throws IOException {
        return api.cancel(r);
    }

    public KetQuaTraCuuHoaDon search(YeuCauTraCuuHoaDon r) throws IOException {
        return api.search(r);
    }

    public List<DuLieuThue> taxes() throws IOException {
        return api.taxes();
    }
}
