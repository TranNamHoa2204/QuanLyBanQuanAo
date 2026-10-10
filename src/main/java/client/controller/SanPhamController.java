package client.controller;

import java.io.IOException;
import java.util.List;
import client.api.SanPhamApi;
import common.dto.*;

public final class SanPhamController {
    private final SanPhamApi api;
    public SanPhamController(SanPhamApi api) { this.api = api; }
    public SanPhamSearchResult search(SanPhamSearchRequest request) throws IOException { return api.search(request); }
    public SanPhamDetailDTO detail(String maSP) throws IOException { return api.detail(maSP); }
    public List<LoaiSanPhamDTO> categories() throws IOException { return api.categories(); }
    public List<ThuongHieuDTO> brands() throws IOException { return api.brands(); }
}
