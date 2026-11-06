package common.dto.sanpham;

/** DTO độc lập với JPA, truyền bằng JSON qua Socket. */
public class YeuCauChiTietSanPham {
    private String maSP;

    public YeuCauChiTietSanPham() {
    }

    public String getMaSP() {
        return maSP;
    }

    public void setMaSP(String maSP) {
        this.maSP = maSP;
    }
}
