package common.dto.phieunhap;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import common.enums.TrangThaiPhieuNhap;

public class DuLieuPhieuNhap {
    private String maPhieuNhap, maNCC, tenNCC, maNVTao, tenNVTao, maNVXacNhan, tenNVXacNhan;
    private LocalDateTime ngayLap, ngayXacNhan;
    private TrangThaiPhieuNhap trangThai;
    private BigDecimal tongTien;
    private Long phienBan;

    public DuLieuPhieuNhap() {
    }

    public String getMaPhieuNhap() {
        return maPhieuNhap;
    }

    public void setMaPhieuNhap(String value) {
        this.maPhieuNhap = value;
    }

    public String getMaNCC() {
        return maNCC;
    }

    public void setMaNCC(String value) {
        this.maNCC = value;
    }

    public String getTenNCC() {
        return tenNCC;
    }

    public void setTenNCC(String value) {
        this.tenNCC = value;
    }

    public String getMaNVTao() {
        return maNVTao;
    }

    public void setMaNVTao(String value) {
        this.maNVTao = value;
    }

    public String getTenNVTao() {
        return tenNVTao;
    }

    public void setTenNVTao(String value) {
        this.tenNVTao = value;
    }

    public String getMaNVXacNhan() {
        return maNVXacNhan;
    }

    public void setMaNVXacNhan(String value) {
        this.maNVXacNhan = value;
    }

    public String getTenNVXacNhan() {
        return tenNVXacNhan;
    }

    public void setTenNVXacNhan(String value) {
        this.tenNVXacNhan = value;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime value) {
        this.ngayLap = value;
    }

    public LocalDateTime getNgayXacNhan() {
        return ngayXacNhan;
    }

    public void setNgayXacNhan(LocalDateTime value) {
        this.ngayXacNhan = value;
    }

    public TrangThaiPhieuNhap getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiPhieuNhap value) {
        this.trangThai = value;
    }

    public BigDecimal getTongTien() {
        return tongTien;
    }

    public void setTongTien(BigDecimal value) {
        this.tongTien = value;
    }

    public Long getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Long value) {
        this.phienBan = value;
    }
}
