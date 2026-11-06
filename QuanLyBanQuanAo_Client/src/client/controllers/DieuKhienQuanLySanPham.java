package client.controllers;

import java.io.IOException;
import java.util.List;

import client.api.GoiDichVuLichSuKho;
import client.api.GoiDichVuSanPham;
import common.dto.danhmuc.DuLieuLoaiSanPham;
import common.dto.danhmuc.DuLieuThuongHieu;
import common.dto.danhmuc.YeuCauLuuDanhMuc;
import common.dto.kho.DuLieuLichSuKho;
import common.dto.kho.YeuCauDieuChinhTonKho;
import common.dto.sanpham.DuLieuBienThe;
import common.dto.sanpham.DuLieuDayDuSanPham;
import common.dto.sanpham.YeuCauLuuSanPham;

public final class DieuKhienQuanLySanPham {
    private final GoiDichVuSanPham api;
    private final GoiDichVuLichSuKho stockHistory;

    public DieuKhienQuanLySanPham(GoiDichVuSanPham api) {
        this(api, null);
    }

    public DieuKhienQuanLySanPham(GoiDichVuSanPham api, GoiDichVuLichSuKho stockHistory) {
        this.api = api;
        this.stockHistory = stockHistory;
    }

    public GoiDichVuSanPham products() {
        return api;
    }

    public DuLieuDayDuSanPham detail(String id) throws IOException {
        return api.detail(id);
    }

    public List<DuLieuLoaiSanPham> categories() throws IOException {
        return api.categories();
    }

    public List<DuLieuThuongHieu> brands() throws IOException {
        return api.brands();
    }

    public String product(YeuCauLuuSanPham r, int mode) throws IOException {
        return api.saveProduct(r, mode);
    }

    public String catalog(YeuCauLuuDanhMuc r, boolean brand, boolean create) throws IOException {
        return brand ? api.saveBrand(r, create) : api.saveCategory(r, create);
    }

    public DuLieuBienThe variant(DuLieuBienThe r, boolean create) throws IOException {
        return api.saveVariant(r, create);
    }

    public DuLieuLichSuKho adjustStock(YeuCauDieuChinhTonKho request) throws IOException {
        if (stockHistory == null) {
            throw new IllegalStateException("Lịch sử kho chưa được cấu hình.");
        }
        return stockHistory.adjust(request);
    }
}
