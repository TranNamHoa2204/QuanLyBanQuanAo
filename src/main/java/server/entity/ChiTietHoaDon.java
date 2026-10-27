package server.entity;

import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Entity JPA phía server; không truyền entity qua Socket. */
@Entity
@Table(name = "ChiTietHoaDon", schema = "dbo", uniqueConstraints = @UniqueConstraint(columnNames = { "maHD",
        "maBienThe" }))
public class ChiTietHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long maCTHD;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHD", nullable = false)
    private HoaDon hoaDon;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maBienThe", nullable = false)
    private BienThe bienThe;
    @Column(nullable = false)
    private int soLuong;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal donGia;
    @Column(insertable = false, updatable = false, precision = 18, scale = 2)
    private BigDecimal thanhTien;

    public ChiTietHoaDon() {
    }

    public Long getMaCTHD() {
        return maCTHD;
    }

    public void setMaCTHD(Long maCTHD) {
        this.maCTHD = maCTHD;
    }

    public HoaDon getHoaDon() {
        return hoaDon;
    }

    public void setHoaDon(HoaDon hoaDon) {
        this.hoaDon = hoaDon;
    }

    public BienThe getBienThe() {
        return bienThe;
    }

    public void setBienThe(BienThe bienThe) {
        this.bienThe = bienThe;
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
