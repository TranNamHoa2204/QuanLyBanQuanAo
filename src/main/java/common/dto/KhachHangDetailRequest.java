package common.dto;

/** Yêu cầu xem một hồ sơ khách hàng theo mã. */
public class KhachHangDetailRequest {
    private String maKH;
    public KhachHangDetailRequest() { }
    public String getMaKH() { return maKH; }
    public void setMaKH(String maKH) { this.maKH = maKH; }
}
