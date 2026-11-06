package common.dto.nhacungcap;

import java.util.ArrayList;
import java.util.List;

public class KetQuaTraCuuNhaCungCap {
    private List<DuLieuNhaCungCap> items = new ArrayList<DuLieuNhaCungCap>();
    private long total;
    private int page, pageSize;

    public KetQuaTraCuuNhaCungCap() {
    }

    public List<DuLieuNhaCungCap> getItems() {
        return items;
    }

    public void setItems(List<DuLieuNhaCungCap> value) {
        this.items = value;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long value) {
        this.total = value;
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
