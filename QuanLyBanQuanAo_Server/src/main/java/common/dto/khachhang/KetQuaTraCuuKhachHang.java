package common.dto.khachhang;

import java.util.ArrayList;
import java.util.List;

/** Một trang dữ liệu khách hàng, bao gồm cả khách đã ngừng nếu không lọc trạng thái. */
public class KetQuaTraCuuKhachHang {
    private List<DuLieuKhachHang> items = new ArrayList<>();
    private long total;
    private int page;
    private int pageSize;

    public KetQuaTraCuuKhachHang() {
    }

    public List<DuLieuKhachHang> getItems() {
        return items;
    }

    public void setItems(List<DuLieuKhachHang> items) {
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
