package server.entity;
import javax.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Entity JPA phía server; không truyền entity qua Socket. */
@Entity
@Table(name = "KhuyenMai", schema = "dbo")
public class KhuyenMai {
    @Id @Column(length = 20)
    private String maKM;
    @Nationalized @Column(nullable = false, length = 200)
    private String tenKM;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal phanTramGiam;
    @Column(nullable = false)
    private LocalDate ngayBatDau;
    @Column(nullable = false)
    private LocalDate ngayKetThuc;
    @Column(nullable = false)
    private boolean trangThai = true;

    public KhuyenMai() {
    }

    public String getMaKM() {
        return maKM;
    }

    public void setMaKM(String maKM) {
        this.maKM = maKM;
    }

    public String getTenKM() {
        return tenKM;
    }

    public void setTenKM(String tenKM) {
        this.tenKM = tenKM;
    }

    public BigDecimal getPhanTramGiam() {
        return phanTramGiam;
    }

    public void setPhanTramGiam(BigDecimal phanTramGiam) {
        this.phanTramGiam = phanTramGiam;
    }

    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDate ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }
}
