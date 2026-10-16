package common.dto;

/** Bộ lọc danh sách khuyến mãi; quyền bán chỉ nhận chương trình đang có hiệu lực. */
public class KhuyenMaiSearchRequest {
    private String tuKhoa;
    private Boolean trangThai;
    private int page;
    private int pageSize = 20;

    public KhuyenMaiSearchRequest() { }
    public String getTuKhoa() { return tuKhoa; }
    public void setTuKhoa(String tuKhoa) { this.tuKhoa = tuKhoa; }
    public Boolean getTrangThai() { return trangThai; }
    public void setTrangThai(Boolean trangThai) { this.trangThai = trangThai; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
