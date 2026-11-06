package common.dto.sanpham;

/** DTO độc lập với JPA, truyền bằng JSON qua Socket. */
public class DuLieuDayDuSanPham {
    private DuLieuSanPham sanPham;
    private java.util.List<DuLieuBienThe> bienThe = new java.util.ArrayList<>();
    private java.util.List<DuLieuAnhSanPham> hinhAnh = new java.util.ArrayList<>();

    public DuLieuDayDuSanPham() {
    }

    public DuLieuSanPham getSanPham() {
        return sanPham;
    }

    public void setSanPham(DuLieuSanPham sanPham) {
        this.sanPham = sanPham;
    }

    public java.util.List<DuLieuBienThe> getBienThe() {
        return bienThe;
    }

    public void setBienThe(java.util.List<DuLieuBienThe> bienThe) {
        this.bienThe = bienThe;
    }

    public java.util.List<DuLieuAnhSanPham> getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(java.util.List<DuLieuAnhSanPham> hinhAnh) {
        this.hinhAnh = hinhAnh;
    }
}
