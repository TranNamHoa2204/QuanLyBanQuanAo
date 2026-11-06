package common.dto.phieunhap;

public class YeuCauXacNhanPhieuNhap {
    private String maPhieuNhap, operationId;
    private Long phienBan;

    public YeuCauXacNhanPhieuNhap() {
    }

    public String getMaPhieuNhap() {
        return maPhieuNhap;
    }

    public void setMaPhieuNhap(String value) {
        this.maPhieuNhap = value;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String value) {
        this.operationId = value;
    }

    public Long getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Long value) {
        this.phienBan = value;
    }
}
