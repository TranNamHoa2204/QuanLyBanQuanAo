package common.dto;

public class HoaDonSearchRequest {
    private String maHD;
    private common.enums.TrangThaiHoaDon trangThai;
    private int page;
    private int pageSize = 20;

    public HoaDonSearchRequest() {
    }

    public String getMaHD() {
        return maHD;
    }

    public void setMaHD(String maHD) {
        this.maHD = maHD;
    }

    public common.enums.TrangThaiHoaDon getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(common.enums.TrangThaiHoaDon trangThai) {
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
