package common.dto.phieunhap;

import java.math.BigDecimal;

public class DuLieuChiTietPhieuNhap {
    private String maBienThe, tenSP, mauSac, kichCo;
    private int soLuong;
    private BigDecimal donGiaNhap, thanhTien;

    public DuLieuChiTietPhieuNhap() {
    }

    public String getMaBienThe() {
        return maBienThe;
    }

    public void setMaBienThe(String value) {
        this.maBienThe = value;
    }

    public String getTenSP() {
        return tenSP;
    }

    public void setTenSP(String value) {
        this.tenSP = value;
    }

    public String getMauSac() {
        return mauSac;
    }

    public void setMauSac(String value) {
        this.mauSac = value;
    }

    public String getKichCo() {
        return kichCo;
    }

    public void setKichCo(String value) {
        this.kichCo = value;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int value) {
        this.soLuong = value;
    }

    public BigDecimal getDonGiaNhap() {
        return donGiaNhap;
    }

    public void setDonGiaNhap(BigDecimal value) {
        this.donGiaNhap = value;
    }

    public BigDecimal getThanhTien() {
        return thanhTien;
    }

    public void setThanhTien(BigDecimal value) {
        this.thanhTien = value;
    }
}
