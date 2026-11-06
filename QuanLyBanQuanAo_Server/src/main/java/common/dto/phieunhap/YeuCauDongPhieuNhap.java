package common.dto.phieunhap;

import java.math.BigDecimal;

public class YeuCauDongPhieuNhap {
    private String maPhieuNhap, maBienThe, operationId;
    private Long phienBan;
    private Integer soLuong;
    private BigDecimal donGiaNhap;

    public YeuCauDongPhieuNhap() {
    }

    public String getMaPhieuNhap() {
        return maPhieuNhap;
    }

    public void setMaPhieuNhap(String value) {
        this.maPhieuNhap = value;
    }

    public String getMaBienThe() {
        return maBienThe;
    }

    public void setMaBienThe(String value) {
        this.maBienThe = value;
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

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer value) {
        this.soLuong = value;
    }

    public BigDecimal getDonGiaNhap() {
        return donGiaNhap;
    }

    public void setDonGiaNhap(BigDecimal value) {
        this.donGiaNhap = value;
    }
}
