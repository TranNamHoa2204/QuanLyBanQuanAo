package common.dto.khachhang;

/** Bộ lọc danh sách khách hàng; không chứa entity hay dữ liệu nhạy cảm. */
public class YeuCauTraCuuKhachHang {
    private String tuKhoa;
    private Boolean trangThai;
    private int page;
    private int pageSize = 20;

    public YeuCauTraCuuKhachHang() {
    }

    public String getTuKhoa() {
        return tuKhoa;
    }

    public void setTuKhoa(String tuKhoa) {
        this.tuKhoa = tuKhoa;
    }

    public Boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Boolean trangThai) {
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
