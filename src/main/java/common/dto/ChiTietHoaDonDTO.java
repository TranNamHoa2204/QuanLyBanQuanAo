package common.dto;

import java.math.BigDecimal;

/** Dữ liệu trao đổi; không phụ thuộc entity/JPA. Không ghi mật khẩu hoặc token vào log. */
public class ChiTietHoaDonDTO {
    private Long maCTHD;
    private String maHD;
    private String maBienThe;
    private int soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private String tenSP;
    private String mauSac;
    private String kichCo;
    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }
    public String getMauSac() { return mauSac; }
    public void setMauSac(String mauSac) { this.mauSac = mauSac; }
    public String getKichCo() { return kichCo; }
    public void setKichCo(String kichCo) { this.kichCo = kichCo; }

    public ChiTietHoaDonDTO() {
    }

    public Long getMaCTHD() {
        return maCTHD;
    }

    public void setMaCTHD(Long maCTHD) {
        this.maCTHD = maCTHD;
    }

    public String getMaHD() {
        return maHD;
    }

    public void setMaHD(String maHD) {
        this.maHD = maHD;
    }

    public String getMaBienThe() {
        return maBienThe;
    }

    public void setMaBienThe(String maBienThe) {
        this.maBienThe = maBienThe;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }

    public BigDecimal getThanhTien() {
        return thanhTien;
    }

    public void setThanhTien(BigDecimal thanhTien) {
        this.thanhTien = thanhTien;
    }
}
