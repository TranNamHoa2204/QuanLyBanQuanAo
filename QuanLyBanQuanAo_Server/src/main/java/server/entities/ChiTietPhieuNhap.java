package server.entities;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "ChiTietPhieuNhap", schema = "dbo", uniqueConstraints = @UniqueConstraint(name = "UQ_ChiTietPhieuNhap_Phieu_BienThe", columnNames = {
        "maPhieuNhap", "maBienThe" }))
public class ChiTietPhieuNhap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long maCTPN;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maPhieuNhap", nullable = false)
    private PhieuNhap phieuNhap;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maBienThe", nullable = false)
    private BienThe bienThe;
    @Column(nullable = false)
    private int soLuong;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal donGiaNhap;
    @Column(insertable = false, updatable = false, precision = 18, scale = 2)
    private BigDecimal thanhTien;

    public ChiTietPhieuNhap() {
    }

    public Long getMaCTPN() {
        return maCTPN;
    }

    public void setMaCTPN(Long value) {
        this.maCTPN = value;
    }

    public PhieuNhap getPhieuNhap() {
        return phieuNhap;
    }

    public void setPhieuNhap(PhieuNhap value) {
        this.phieuNhap = value;
    }

    public BienThe getBienThe() {
        return bienThe;
    }

    public void setBienThe(BienThe value) {
        this.bienThe = value;
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
