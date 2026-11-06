package common.dto.phieunhap;

import java.util.ArrayList;
import java.util.List;

public class KetQuaTraCuuPhieuNhap {
    private List<DuLieuPhieuNhap> items = new ArrayList<DuLieuPhieuNhap>();
    private long total;
    private int page, pageSize;

    public KetQuaTraCuuPhieuNhap() {
    }

    public List<DuLieuPhieuNhap> getItems() {
        return items;
    }

    public void setItems(List<DuLieuPhieuNhap> value) {
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
