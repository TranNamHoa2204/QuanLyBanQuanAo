package client.controllers;

import java.io.IOException;

import client.api.GoiDichVuKhachHang;
import common.dto.khachhang.DuLieuKhachHang;
import common.dto.khachhang.KetQuaTraCuuKhachHang;
import common.dto.khachhang.YeuCauLuuKhachHang;
import common.dto.khachhang.YeuCauTraCuuKhachHang;
import common.dto.khachhang.YeuCauTrangThaiKhachHang;

public final class DieuKhienKhachHang {
    private final GoiDichVuKhachHang api;

    public DieuKhienKhachHang(GoiDichVuKhachHang api) {
        this.api = api;
    }

    public KetQuaTraCuuKhachHang search(YeuCauTraCuuKhachHang request) throws IOException {
        return api.search(request);
    }

    public DuLieuKhachHang detail(String id) throws IOException {
        return api.detail(id);
    }

    public DuLieuKhachHang create(DuLieuKhachHang request) throws IOException {
        return api.create(request);
    }

    public DuLieuKhachHang update(YeuCauLuuKhachHang request) throws IOException {
        return api.update(request);
    }

    public DuLieuKhachHang setActive(YeuCauTrangThaiKhachHang request, boolean active) throws IOException {
        return api.setActive(request, active);
    }
}
