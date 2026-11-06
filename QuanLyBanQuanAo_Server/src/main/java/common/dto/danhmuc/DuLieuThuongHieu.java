package common.dto.danhmuc;

/** Dữ liệu trao đổi; không phụ thuộc entity/JPA. Không ghi mật khẩu hoặc token vào log. */
public class DuLieuThuongHieu {
    private String maThuongHieu;
    private String tenThuongHieu;

    public DuLieuThuongHieu() {
    }

    public String getMaThuongHieu() {
        return maThuongHieu;
    }

    public void setMaThuongHieu(String maThuongHieu) {
        this.maThuongHieu = maThuongHieu;
    }

    public String getTenThuongHieu() {
        return tenThuongHieu;
    }

    public void setTenThuongHieu(String tenThuongHieu) {
        this.tenThuongHieu = tenThuongHieu;
    }
}
