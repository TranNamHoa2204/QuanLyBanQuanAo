package common.dto.kho;

import java.util.ArrayList;
import java.util.List;

/** Trang kết quả lịch sử kho. */
public class KetQuaTraCuuLichSuKho {
    private List<DuLieuLichSuKho> items = new ArrayList<DuLieuLichSuKho>();
    private long total;
    private int page;
    private int pageSize;

    public KetQuaTraCuuLichSuKho() {
    }

    public List<DuLieuLichSuKho> getItems() {
        return items;
    }

    public void setItems(List<DuLieuLichSuKho> items) {
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
