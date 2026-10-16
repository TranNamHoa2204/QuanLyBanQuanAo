package client.controller;

import java.io.IOException;
import java.util.List;
import client.api.HoaDonApi;
import common.dto.*;

/** Điều phối bán hàng; SwingWorker do view quản lý, nghiệp vụ tin cậy nằm trên server. */
public final class BanHangController {
    private final HoaDonApi api;
    public BanHangController(HoaDonApi api) { this.api = api; }
    public HoaDonDetailDTO create(HoaDonCreateRequest r) throws IOException { return api.create(r); }
    public HoaDonDetailDTO detail(String id) throws IOException { return api.detail(id); }
    public HoaDonDetailDTO line(HoaDonLineRequest r, int mode) throws IOException { return api.line(r, mode); }
    public HoaDonDetailDTO pay(HoaDonThanhToanRequest r) throws IOException { return api.pay(r); }
    public HoaDonDetailDTO cancel(HoaDonVersionRequest r) throws IOException { return api.cancel(r); }
    public HoaDonSearchResult search(HoaDonSearchRequest r) throws IOException { return api.search(r); }
    public List<ThueDTO> taxes() throws IOException { return api.taxes(); }
}
