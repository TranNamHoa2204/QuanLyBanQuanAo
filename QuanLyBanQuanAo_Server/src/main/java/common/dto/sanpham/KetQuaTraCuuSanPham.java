package common.dto.sanpham;

/** DTO độc lập với JPA, truyền bằng JSON qua Socket. */
public class KetQuaTraCuuSanPham {
    private java.util.List<DuLieuSanPham> items = new java.util.ArrayList<>();
    private long total;
    private int page;
    private int pageSize;

    public KetQuaTraCuuSanPham() {
    }

    public java.util.List<DuLieuSanPham> getItems() {
        return items;
    }

    public void setItems(java.util.List<DuLieuSanPham> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
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
