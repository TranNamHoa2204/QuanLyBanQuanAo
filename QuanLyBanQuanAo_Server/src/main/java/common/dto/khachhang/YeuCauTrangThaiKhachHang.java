package common.dto.khachhang;

/** Ngừng/khôi phục có điều kiện theo trạng thái đã đọc trước đó. */
public class YeuCauTrangThaiKhachHang {
    private String maKH;
    private Boolean expectedTrangThai;

    public YeuCauTrangThaiKhachHang() {
    }

    public String getMaKH() {
        return maKH;
    }

    public void setMaKH(String maKH) {
        this.maKH = maKH;
    }

    public Boolean getExpectedTrangThai() {
        return expectedTrangThai;
    }

    public void setExpectedTrangThai(Boolean expectedTrangThai) {
        this.expectedTrangThai = expectedTrangThai;
    }
}
