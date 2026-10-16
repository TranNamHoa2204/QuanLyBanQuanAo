package common.dto;

import java.util.ArrayList;
import java.util.List;

/** Một trang kết quả quản lý nhân viên, không chứa password/hash. */
public class NhanVienSearchResult {
    private List<NhanVienRecordDTO> items = new ArrayList<>();
    private long total;
    private int page;
    private int pageSize;

    public NhanVienSearchResult() { }
    public List<NhanVienRecordDTO> getItems() { return items; }
    public void setItems(List<NhanVienRecordDTO> items) { this.items = items; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
