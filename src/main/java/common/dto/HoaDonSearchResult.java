package common.dto;

public class HoaDonSearchResult {
    private java.util.List<HoaDonDTO> items = new java.util.ArrayList<>();
    private long total;
    private int page;
    private int pageSize;
    public HoaDonSearchResult() { }
    public java.util.List<HoaDonDTO> getItems() { return items; }
    public void setItems(java.util.List<HoaDonDTO> items) { this.items = items; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
