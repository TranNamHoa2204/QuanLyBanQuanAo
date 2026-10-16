package common.dto;

/** Yêu cầu xem một nhân viên theo mã. */
public class NhanVienDetailRequest {
    private String maNV;
    public NhanVienDetailRequest() { }
    public String getMaNV() { return maNV; }
    public void setMaNV(String maNV) { this.maNV = maNV; }
}
