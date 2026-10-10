package common.dto;

/** DTO độc lập với JPA, truyền bằng JSON qua Socket. */
public class SanPhamDetailDTO {
    private SanPhamDTO sanPham;
    private java.util.List<BienTheDTO> bienThe = new java.util.ArrayList<>();
    private java.util.List<AnhSanPhamDTO> hinhAnh = new java.util.ArrayList<>();
    public SanPhamDetailDTO() { }
    public SanPhamDTO getSanPham() { return sanPham; }
    public void setSanPham(SanPhamDTO sanPham) { this.sanPham = sanPham; }
    public java.util.List<BienTheDTO> getBienThe() { return bienThe; }
    public void setBienThe(java.util.List<BienTheDTO> bienThe) { this.bienThe = bienThe; }
    public java.util.List<AnhSanPhamDTO> getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(java.util.List<AnhSanPhamDTO> hinhAnh) { this.hinhAnh = hinhAnh; }
}
