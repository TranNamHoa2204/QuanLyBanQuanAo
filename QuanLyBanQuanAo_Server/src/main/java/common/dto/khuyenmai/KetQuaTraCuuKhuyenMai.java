package common.dto.khuyenmai;

import java.util.ArrayList;
import java.util.List;

/** Trang kết quả gồm cờ lưu và trạng thái hiệu lực được server suy ra. */
public class KetQuaTraCuuKhuyenMai {
    private List<DuLieuKhuyenMai> items = new ArrayList<>();
    private long total;
    private int page;
    private int pageSize;

    public KetQuaTraCuuKhuyenMai() {
    }

    public List<DuLieuKhuyenMai> getItems() {
        return items;
    }

    public void setItems(List<DuLieuKhuyenMai> items) {
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
