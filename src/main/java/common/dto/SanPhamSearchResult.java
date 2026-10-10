package common.dto;

/** DTO độc lập với JPA, truyền bằng JSON qua Socket. */
public class SanPhamSearchResult {
    private java.util.List<SanPhamDTO> items = new java.util.ArrayList<>();
    private long total;
    private int page;
    private int pageSize;
    public SanPhamSearchResult() { }
    public java.util.List<SanPhamDTO> getItems() { return items; }
    public void setItems(java.util.List<SanPhamDTO> items) { this.items = items; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
