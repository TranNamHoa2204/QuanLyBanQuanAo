package common.dto.sanpham;

/** Dữ liệu ảnh trao đổi qua Socket; không chứa entity JPA. */
public class DuLieuAnhSanPham {
    private String maAnh;
    private String maSP;
    private String mauSac;
    private String duongDan;
    private int thuTu;
    private boolean anhChinh;

    public DuLieuAnhSanPham() {
    }

    public String getMaAnh() {
        return maAnh;
    }

    public void setMaAnh(String maAnh) {
        this.maAnh = maAnh;
    }

    public String getMaSP() {
        return maSP;
    }

    public void setMaSP(String maSP) {
        this.maSP = maSP;
    }

    public String getMauSac() {
        return mauSac;
    }

    public void setMauSac(String mauSac) {
        this.mauSac = mauSac;
    }

    public String getDuongDan() {
        return duongDan;
    }

    public void setDuongDan(String duongDan) {
        this.duongDan = duongDan;
    }

    public int getThuTu() {
        return thuTu;
    }

    public void setThuTu(int thuTu) {
        this.thuTu = thuTu;
    }

    public boolean getAnhChinh() {
        return anhChinh;
    }

    public void setAnhChinh(boolean anhChinh) {
        this.anhChinh = anhChinh;
    }
}
