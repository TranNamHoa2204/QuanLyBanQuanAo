package common.dto;

/** Yêu cầu xem chi tiết theo mã khuyến mãi. */
public class KhuyenMaiDetailRequest {
    private String maKM;
    public KhuyenMaiDetailRequest() { }
    public String getMaKM() { return maKM; }
    public void setMaKM(String maKM) { this.maKM = maKM; }
}
