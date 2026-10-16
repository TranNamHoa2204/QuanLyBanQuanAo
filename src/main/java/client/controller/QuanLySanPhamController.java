package client.controller;

import java.io.IOException;
import java.util.List;
import client.api.SanPhamApi;
import client.api.LichSuKhoApi;
import common.dto.*;

public final class QuanLySanPhamController {
    private final SanPhamApi api;
    private final LichSuKhoApi stockHistory;
    public QuanLySanPhamController(SanPhamApi api) { this(api, null); }
    public QuanLySanPhamController(SanPhamApi api, LichSuKhoApi stockHistory) {
        this.api = api; this.stockHistory = stockHistory;
    }
    public SanPhamApi products() { return api; }
    public SanPhamDetailDTO detail(String id) throws IOException { return api.detail(id); }
    public List<LoaiSanPhamDTO> categories() throws IOException { return api.categories(); }
    public List<ThuongHieuDTO> brands() throws IOException { return api.brands(); }
    public String product(SanPhamSaveRequest r, int mode) throws IOException { return api.saveProduct(r, mode); }
    public String catalog(DanhMucSaveRequest r, boolean brand, boolean create) throws IOException {
        return brand ? api.saveBrand(r, create) : api.saveCategory(r, create);
    }
    public BienTheDTO variant(BienTheDTO r, boolean create) throws IOException { return api.saveVariant(r, create); }
    public LichSuKhoDTO adjustStock(DieuChinhTonKhoRequest request) throws IOException {
        if (stockHistory == null) { throw new IllegalStateException("Lịch sử kho chưa được cấu hình."); }
        return stockHistory.adjust(request);
    }
}
