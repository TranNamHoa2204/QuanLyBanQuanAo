package common.dto.nhanvien;

/** Chuyển trạng thái nhân viên với điều kiện trạng thái cũ. */
public class YeuCauTrangThaiNhanVien {
    private String maNV;
    private Boolean expectedTrangThai;

    public YeuCauTrangThaiNhanVien() {
    }

    public String getMaNV() {
        return maNV;
    }

    public void setMaNV(String maNV) {
        this.maNV = maNV;
    }

    public Boolean getExpectedTrangThai() {
        return expectedTrangThai;
    }

    public void setExpectedTrangThai(Boolean expectedTrangThai) {
        this.expectedTrangThai = expectedTrangThai;
    }
}
