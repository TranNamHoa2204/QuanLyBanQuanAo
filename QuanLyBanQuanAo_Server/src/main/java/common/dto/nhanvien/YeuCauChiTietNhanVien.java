package common.dto.nhanvien;

/** Yêu cầu xem một nhân viên theo mã. */
public class YeuCauChiTietNhanVien {
    private String maNV;

    public YeuCauChiTietNhanVien() {
    }

    public String getMaNV() {
        return maNV;
    }

    public void setMaNV(String maNV) {
        this.maNV = maNV;
    }
}
