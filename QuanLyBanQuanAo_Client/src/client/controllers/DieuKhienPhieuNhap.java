package client.controllers;

import java.io.IOException;

import client.api.GoiDichVuPhieuNhap;
import common.dto.phieunhap.DuLieuDayDuPhieuNhap;
import common.dto.phieunhap.KetQuaTraCuuPhieuNhap;
import common.dto.phieunhap.YeuCauDongPhieuNhap;
import common.dto.phieunhap.YeuCauPhienBanPhieuNhap;
import common.dto.phieunhap.YeuCauTaoPhieuNhap;
import common.dto.phieunhap.YeuCauTraCuuPhieuNhap;
import common.dto.phieunhap.YeuCauXacNhanPhieuNhap;

public final class DieuKhienPhieuNhap {
    private final GoiDichVuPhieuNhap api;

    public DieuKhienPhieuNhap(GoiDichVuPhieuNhap api) {
        this.api = api;
    }

    public KetQuaTraCuuPhieuNhap search(YeuCauTraCuuPhieuNhap r) throws IOException {
        return api.search(r);
    }

    public DuLieuDayDuPhieuNhap detail(String id) throws IOException {
        return api.detail(id);
    }

    public DuLieuDayDuPhieuNhap create(YeuCauTaoPhieuNhap r) throws IOException {
        return api.create(r);
    }

    public DuLieuDayDuPhieuNhap add(YeuCauDongPhieuNhap r) throws IOException {
        return api.addLine(r);
    }

    public DuLieuDayDuPhieuNhap update(YeuCauDongPhieuNhap r) throws IOException {
        return api.updateLine(r);
    }

    public DuLieuDayDuPhieuNhap remove(YeuCauDongPhieuNhap r) throws IOException {
        return api.removeLine(r);
    }

    public DuLieuDayDuPhieuNhap confirm(YeuCauXacNhanPhieuNhap r) throws IOException {
        return api.confirm(r);
    }

    public DuLieuDayDuPhieuNhap cancel(YeuCauPhienBanPhieuNhap r) throws IOException {
        return api.cancel(r);
    }
}
