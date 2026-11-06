package common.dto.hoadon;

/** Gắn/bỏ khách hàng trên hóa đơn nháp bằng điều kiện phiên bản đã đọc. */
public class YeuCauGanKhachHangHoaDon {
    private String maHD;
    private Long phienBan;
    private String maKH;

    public YeuCauGanKhachHangHoaDon() {
    }

    public String getMaHD() {
        return maHD;
    }

    public void setMaHD(String maHD) {
        this.maHD = maHD;
    }

    public Long getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Long phienBan) {
        this.phienBan = phienBan;
    }

    public String getMaKH() {
        return maKH;
    }

    public void setMaKH(String maKH) {
        this.maKH = maKH;
    }
}
