package common.dto;

/** DTO độc lập với JPA, truyền bằng JSON qua Socket. */
public class SanPhamDetailRequest {
    private String maSP;
    public SanPhamDetailRequest() { }
    public String getMaSP() { return maSP; }
    public void setMaSP(String maSP) { this.maSP = maSP; }
}
