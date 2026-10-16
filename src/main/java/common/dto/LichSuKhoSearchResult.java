package common.dto;

import java.util.ArrayList;
import java.util.List;

/** Trang kết quả lịch sử kho. */
public class LichSuKhoSearchResult {
    private List<LichSuKhoDTO> items = new ArrayList<LichSuKhoDTO>();
    private long total;
    private int page;
    private int pageSize;
    public LichSuKhoSearchResult() { }
    public List<LichSuKhoDTO> getItems() { return items; }
    public void setItems(List<LichSuKhoDTO> items) { this.items = items; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
