package common.dto.hoadon;

public class DuLieuDayDuHoaDon {
    private DuLieuHoaDon hoaDon;
    private java.util.List<DuLieuChiTietHoaDon> chiTiet = new java.util.ArrayList<>();

    public DuLieuDayDuHoaDon() {
    }

    public DuLieuHoaDon getHoaDon() {
        return hoaDon;
    }

    public void setHoaDon(DuLieuHoaDon hoaDon) {
        this.hoaDon = hoaDon;
    }

    public java.util.List<DuLieuChiTietHoaDon> getChiTiet() {
        return chiTiet;
    }

    public void setChiTiet(java.util.List<DuLieuChiTietHoaDon> chiTiet) {
        this.chiTiet = chiTiet;
    }
}
