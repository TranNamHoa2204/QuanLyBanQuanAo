package common.dto.nhacungcap;

public class YeuCauTraCuuNhaCungCap {
    private String tuKhoa;
    private Boolean trangThai;
    private int page;
    private int pageSize = 20;

    public YeuCauTraCuuNhaCungCap() {
    }

    public String getTuKhoa() {
        return tuKhoa;
    }

    public void setTuKhoa(String value) {
        this.tuKhoa = value;
    }

    public Boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Boolean value) {
        this.trangThai = value;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int value) {
        this.page = value;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int value) {
        this.pageSize = value;
    }
}
