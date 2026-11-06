package common.dto.hoadon;

import common.enums.TrangThaiHoaDon;

public class YeuCauTraCuuHoaDon {
    private String maHD;
    private TrangThaiHoaDon trangThai;
    private int page;
    private int pageSize = 20;

    public YeuCauTraCuuHoaDon() {
    }

    public String getMaHD() {
        return maHD;
    }

    public void setMaHD(String maHD) {
        this.maHD = maHD;
    }

    public TrangThaiHoaDon getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiHoaDon trangThai) {
        this.trangThai = trangThai;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
