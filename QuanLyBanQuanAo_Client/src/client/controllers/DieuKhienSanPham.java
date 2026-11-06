package client.controllers;

import java.io.IOException;
import java.util.List;

import client.api.GoiDichVuSanPham;
import common.dto.danhmuc.DuLieuLoaiSanPham;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.KetQuaTraCuuSanPham;
import common.dto.sanpham.YeuCauTraCuuSanPham;

public final class DieuKhienSanPham {
    private final GoiDichVuSanPham api;

    public DieuKhienSanPham(GoiDichVuSanPham api) {
        this.api = api;
    }

    public KetQuaTraCuuSanPham search(YeuCauTraCuuSanPham request) throws IOException {
        return api.search(request);
    }

    public DuLieuDayDuSanPham detail(String maSP) throws IOException {
        return api.detail(maSP);
    }

    public List<DuLieuLoaiSanPham> categories() throws IOException {
        return api.categories();
    }

    public List<DuLieuThuongHieu> brands() throws IOException {
        return api.brands();
    }
}
