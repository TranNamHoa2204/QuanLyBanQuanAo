package server.entity;
import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Entity JPA phía server; không truyền entity qua Socket. */
@Entity
@Table(name = "ChiTietHoaDon", schema = "dbo", uniqueConstraints = @UniqueConstraint(columnNames = {"maHD", "maBienThe"}))
public class ChiTietHoaDon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long maCTHD;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHD", nullable = false)
    private HoaDon hoaDon;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maBienThe", nullable = false)
    private BienThe bienThe;
    @Nationalized @Column(name = "tenSPSnapshot", length = 200)
    private String tenSPSnapshot;
    @Nationalized @Column(name = "mauSacSnapshot", length = 50)
    private String mauSacSnapshot;
    @Nationalized @Column(name = "kichCoSnapshot", length = 20)
    private String kichCoSnapshot;
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

    public String getTenSPSnapshot() {
        return tenSPSnapshot;
    }

    public void setTenSPSnapshot(String tenSPSnapshot) {
        this.tenSPSnapshot = tenSPSnapshot;
    }

    public String getMauSacSnapshot() {
        return mauSacSnapshot;
    }

    public void setMauSacSnapshot(String mauSacSnapshot) {
        this.mauSacSnapshot = mauSacSnapshot;
    }

    public String getKichCoSnapshot() {
        return kichCoSnapshot;
    }

    public void setKichCoSnapshot(String kichCoSnapshot) {
        this.kichCoSnapshot = kichCoSnapshot;
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
