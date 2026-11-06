package client.controllers;

import java.io.IOException;

import client.api.GoiDichVuKhuyenMai;
import common.dto.khuyenmai.DuLieuKhuyenMai;
import common.dto.khuyenmai.KetQuaTraCuuKhuyenMai;
import common.dto.khuyenmai.YeuCauLuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTraCuuKhuyenMai;
import common.dto.khuyenmai.YeuCauTrangThaiKhuyenMai;

public final class DieuKhienKhuyenMai {
    private final GoiDichVuKhuyenMai api;

    public DieuKhienKhuyenMai(GoiDichVuKhuyenMai api) {
        this.api = api;
    }

    public KetQuaTraCuuKhuyenMai search(YeuCauTraCuuKhuyenMai request) throws IOException {
        return api.search(request);
    }

    public DuLieuKhuyenMai detail(String id) throws IOException {
        return api.detail(id);
    }

    public DuLieuKhuyenMai create(DuLieuKhuyenMai request) throws IOException {
        return api.create(request);
    }

    public DuLieuKhuyenMai update(YeuCauLuuKhuyenMai request) throws IOException {
        return api.update(request);
    }

    public DuLieuKhuyenMai setActive(YeuCauTrangThaiKhuyenMai request, boolean active) throws IOException {
        return api.setActive(request, active);
    }
}
