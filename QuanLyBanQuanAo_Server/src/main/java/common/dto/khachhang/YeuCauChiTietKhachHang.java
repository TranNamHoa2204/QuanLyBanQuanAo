package common.dto.khachhang;

/** Yêu cầu xem một hồ sơ khách hàng theo mã. */
public class YeuCauChiTietKhachHang {
    private String maKH;

    public YeuCauChiTietKhachHang() {
    }

    public String getMaKH() {
        return maKH;
    }

    public void setMaKH(String maKH) {
        this.maKH = maKH;
    }
}
