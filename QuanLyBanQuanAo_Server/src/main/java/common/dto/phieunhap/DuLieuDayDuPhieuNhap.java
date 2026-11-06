package common.dto.phieunhap;

import java.util.ArrayList;
import java.util.List;

public class DuLieuDayDuPhieuNhap {
    private DuLieuPhieuNhap phieuNhap;
    private List<DuLieuChiTietPhieuNhap> chiTiet = new ArrayList<DuLieuChiTietPhieuNhap>();

    public DuLieuDayDuPhieuNhap() {
    }

    public DuLieuPhieuNhap getPhieuNhap() {
        return phieuNhap;
    }

    public void setPhieuNhap(DuLieuPhieuNhap value) {
        this.phieuNhap = value;
    }

    public List<DuLieuChiTietPhieuNhap> getChiTiet() {
        return chiTiet;
    }

    public void setChiTiet(List<DuLieuChiTietPhieuNhap> value) {
        this.chiTiet = value;
    }
}
